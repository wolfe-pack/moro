package controllers

import java.io.File
import java.math.BigInteger
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.jar.JarFile

import controllers.util.Cache
import org.sameersingh.htmlgen.HTML

import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{Results, IMain}
import scala.tools.nsc.io._
import scala.util.Random
import scala.util.matching.Regex

/**
 * @author sameer
 * @since 12/24/14.
 */
object ScalaIMainInterpreter {
  private val jvmId = java.lang.Math.abs(new Random().nextInt())
  val classCleaner: Regex = "\\W".r

  /*
   * For a given FQ classname, trick the resource finder into telling us the containing jar.
   */
  private def jarPathOfClass(className: String) = try {
    val resource = className.split('.').mkString("/", "/", ".class")
    val path = getClass.getResource(resource).getPath
    val indexOfFile = path.indexOf("file:") + 5
    val indexOfSeparator = path.lastIndexOf('!')
    List(path.substring(indexOfFile, indexOfSeparator))
  }


  lazy val compilerPath = try {
    jarPathOfClass("scala.tools.nsc.Interpreter")
  } catch {
    case e: Throwable =>
      throw new RuntimeException("Unable lo load scala interpreter from classpath (scala-compiler jar is missing?)", e)
  }

  lazy val libPath = try {
    jarPathOfClass("scala.ScalaObject")
  } catch {
    case e: Throwable =>
      throw new RuntimeException("Unable to load scala base object from classpath (scala-library jar is missing?)", e)
  }

  def uniqueId(code: String, idOpt: Option[Int] = Some(jvmId)): String = {
    val digest = MessageDigest.getInstance("SHA-1").digest(code.getBytes())
    val sha = new BigInteger(1, digest).toString(16)
    idOpt match {
      case Some(id) => sha + "_" + id
      case _ => sha
    }
  }

  def codeName(code: String) = "Moro_" + uniqueId(code)

}

class ScalaIMainInterpreter(targetDir: Option[File] = None, classPath: List[String] = List.empty,
                            imports: List[String] = List.empty,
                            classesForJarPath: List[String] = List.empty) extends ScalaInterpreter {

  import ScalaIMainInterpreter._

  private lazy val additionalPath =
    classesForJarPath.map(c =>
      try {
        jarPathOfClass(c)
      } catch {
        case e: Throwable =>
          throw new RuntimeException("Unable to load custom class from classpath (relevant jar for " + c + " missing?)", e)
      }
    ).flatten.toList

  /*
   * Try to guess our app's classpath.
   * This is probably fragile.
   */
  private lazy val impliedClassPath: List[String] = {
    val currentClassPath = this.getClass.getClassLoader.asInstanceOf[URLClassLoader].getURLs.
      filter(_.getProtocol == "file").map(u => new File(u.toURI).getPath).toList

    // if there's just one thing in the classpath, and it's a jar, assume an executable jar.
    classPath ::: currentClassPath ::: (if (currentClassPath.size == 1 && currentClassPath(0).endsWith(".jar")) {
      val jarFile = currentClassPath(0)
      val relativeRoot = new File(jarFile).getParentFile()
      val nestedClassPath = new JarFile(jarFile).getManifest.getMainAttributes.getValue("Class-Path")
      if (nestedClassPath eq null) {
        Nil
      } else {
        nestedClassPath.split(" ").map {
          f => new File(relativeRoot, f).getAbsolutePath
        }.toList
      }
    } else {
      Nil
    })
  }

  private val target = targetDir match {
    case Some(dir) => AbstractFile.getDirectory(dir)
    case None => new VirtualDirectory("(memory)", None)
  }

  private val settings = new Settings
  settings.usejavacp.value = false
  settings.nowarnings.value = true // warnings are exceptions, so disable
  settings.outputDirs.setSingleOutput(target)
  private val pathList = compilerPath ::: libPath ::: additionalPath
  settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
  settings.classpath.value = (pathList ::: impliedClassPath).mkString(File.pathSeparator)
  println(settings.classpath.value)

  private val imainCache = new Cache[String, IMain]

  private def imain(sessionId: String) = imainCache.getOrElseUpdate(sessionId, {
    val im = new IMain(settings)
    imports.foreach(i => im.interpret("import " + i + "\n"))
    im
  })

  def checkCodeCompiled(sessionId: String, code: String): Boolean = checkCodeCompiled(imain(sessionId), code)

  def checkCodeCompiled(im: IMain, code: String): Boolean = {
    im.valueOfTerm(codeName(code)).isDefined
  }

  def interpret(im: IMain, code: String): Unit = {
    val result = im.interpret(code)
    assert(result == Results.Success, "Compilation Failed")
    val anyVar = im.mostRecentVar
    im.interpret("val " + codeName(code) + ": org.sameersingh.htmlgen.HTML = " + anyVar)
    assert(checkCodeCompiled(im, code))
  }

  def getValue(im: IMain, code: String): HTML = {
    val html = im.valueOfTerm(codeName(code))
    assert(html.isDefined)
    html.get.asInstanceOf[HTML]
  }

  def execute(sessionId: String, snippets: Array[String]): org.sameersingh.htmlgen.HTML = {
    val im = imain(sessionId)
    // find first changed snippet
    val firstChanged = snippets.toSeq.indexWhere(code => !checkCodeCompiled(im, code))
    println(s"First Changed Index $firstChanged of ${snippets.size}")
    // compile and run all code after, keeping track of last
    if(firstChanged >= 0) {
      println("First Changed: " + snippets(firstChanged))
      for (idx <- firstChanged until snippets.length) {
        val code = snippets(idx)
        interpret(im, code)
      }
    }
    getValue(im, snippets.last)
  }

  override def compile(sessionId: String, codes: Array[String]): HTML = execute(sessionId, codes)
}