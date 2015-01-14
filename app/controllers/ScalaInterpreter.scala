package controllers

import java.io.File
import java.math.BigInteger
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.jar.JarFile

import controllers.util.Cache

import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.io._
import scala.util.Random
import scala.util.matching.Regex

/**
 * @author sameer
 * @since 12/24/14.
 */
object ScalaInterpreter {
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

  /*
   * Wrap source code in a new class with an apply method.
   */
  def wrapCodeInClass(className: String, code: String, imports: List[String]) = {
    "object " + className + " extends (() => Any) {\n" +
      imports.map(i => "import " + i + "\n").mkString("") +
      code + "\n" +
      "  def apply(): org.sameersingh.htmlgen.HTML = {\n" +
      code + "\n" +
      "  }\n" +
      "}\n"
  }


}

class ScalaInterpreter(targetDir: Option[File] = None, classPath: List[String] = List.empty,
                       imports: List[String] = List.empty,
                       classesForJarPath: List[String] = List.empty) {

  import ScalaInterpreter._

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

  def getClassAndName(sessionId: String, codes: Array[String]): String = {
    assert(codes.length > 0)
    if (codes.length == 1) {
      val id = uniqueId(codes.head)
      val className = "Moro_" + id
      val wrappedCode = wrapCodeInClass(className, codes.head, imports)
      println("code: " + wrappedCode)
      compile(sessionId, wrappedCode, className)
      className
    } else {
      val parentClassName = getClassAndName(sessionId, codes.dropRight(1))
      val code = "import " + parentClassName + "._\n\n" + codes.last
      val id = uniqueId(code)
      val className = "Moro_" + id
      val wrappedCode = wrapCodeInClass(className, code, imports)
      println("code: " + wrappedCode)
      compile(sessionId, wrappedCode, className)
      className
    }
  }

  private val imainCache = new Cache[String, IMain]

  private def imain(sessionId: String) = imainCache.getOrElseUpdate(sessionId, new IMain(settings))

  def checkObjectCompiled(sessionId: String, className: String): Boolean = {
    imain(sessionId).valueOfTerm(className).isDefined
  }

  def compile(sessionId: String, snippet: String, className: String): Unit = {
    if (!checkObjectCompiled(sessionId, className))
      imain(sessionId).interpret(snippet)
      //imain.compileString(snippet)
  }

  def execute[A](sessionId: String, snippets: Array[String]): A = {
    //imain.resetClassLoader()
    val className = getClassAndName(sessionId, snippets)
    //val result = imain.interpret("val ret = " + className + ".apply()")
    // val any = imain.valueOfTerm("ret").get
    // any.asInstanceOf[A]
    val any = imain(sessionId).valueOfTerm(className).get
    any.asInstanceOf[() => A].apply()
  }
}