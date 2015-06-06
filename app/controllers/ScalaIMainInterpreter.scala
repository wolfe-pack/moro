package controllers

import java.io.{StringWriter, PrintWriter, Writer, File}
import java.math.BigInteger
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.jar.JarFile

import controllers.util.Cache
import jline.console.completer.{ArgumentCompleter, Completer}
import org.sameersingh.htmlgen.HTML

import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.interpreter.Completion.{ScalaCompleter, Candidates}
import scala.tools.nsc.{NewLinePrintWriter, Settings}
import scala.tools.nsc.interpreter._
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
  } catch {
    case e: Throwable =>
      throw new RuntimeException(s"Unable to load $className from classpath", e)
  }


  lazy val compilerPath = try {
    jarPathOfClass("scala.tools.nsc.Interpreter")
  } catch {
    case e: Throwable =>
      throw new RuntimeException("Unable lo load scala interpreter from classpath (scala-compiler jar is missing?)", e)
  }

  lazy val libPath = try {
    jarPathOfClass("scala.Array")
  } catch {
    case e: Throwable =>
      throw new RuntimeException("Unable to load scala base object from classpath (scala-library jar is missing?)", e)
  }

  def uniqueId(code: String, idOpt: Option[Int] = Some(jvmId)): String = {
    val digest = MessageDigest.getInstance("SHA-1").digest(code.getBytes())
    val sha = new BigInteger(1, digest).toString(26)
    idOpt match {
      case Some(id) => sha + "_" + id
      case _ => sha
    }
  }

  def codeName(code: String) = "moro_" + uniqueId(code)

}

class ScalaIMainInterpreter(targetDir: Option[File] = None, classPath: List[String] = List.empty,
                            imports: List[String] = List.empty,
                            classesForJarPath: List[String] = List.empty,
                            plugins: List[String] = List.empty,
                            classesForPlugins: List[String] = List.empty) extends ScalaInterpreter {

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

  private lazy val additionalPlugins =
    classesForPlugins.map(c =>
      try {
        jarPathOfClass(c)
      } catch {
        case e: Throwable =>
          throw new RuntimeException("Unable to load custom plugin from classpath (relevant jar for " + c + " missing?)", e)
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

  private val imainCache = new Cache[String, (IMain, StringWriter)]

  private def imainPair(sessionId: String) = imainCache.getOrElseUpdate(sessionId, {
    val w = new StringWriter()
    val settings = new Settings
    settings.usejavacp.value = false
    settings.nowarnings.value = true // warnings are exceptions, so disable
    val target = targetDir match {
        case Some(dir) => AbstractFile.getDirectory(dir)
        case None => new VirtualDirectory("(" + sessionId + ")", None)
      }
    settings.outputDirs.setSingleOutput(target)
    val pathList = compilerPath ::: libPath ::: additionalPath
    settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
    settings.classpath.value = (pathList ::: impliedClassPath).mkString(File.pathSeparator)
    settings.plugin.value = plugins ++ additionalPlugins
    println("Provided cp: " + classPath)
    println("Provided plugin: " + plugins.mkString(", "))
    println(settings.classpath.value)

    val im = new IMain(settings, new NewLinePrintWriter(w, true))
    im.isettings.maxPrintString = 0
    imports.foreach(i => im.interpret("import " + i + "\n"))
    im -> w
  })

  private def imain(sessionId: String) = imainPair(sessionId)._1

  private def imainWriter(sessionId: String) = imainPair(sessionId)._2

  def checkCodeCompiled(sessionId: String, code: String): Boolean = checkCodeCompiled(imain(sessionId), code)

  def checkCodeCompiled(im: IMain, code: String): Boolean = {
    im.valueOfTerm(codeName(code)).isDefined
  }

  def interpret(im: IMain, w: StringWriter, code: String): Unit = {
    w.getBuffer.delete(0, w.getBuffer.length())
    val result = im.interpret(code)
    val log = w.getBuffer.toString
    //w.getBuffer.delete(0, w.getBuffer.length())
    //assert(result == Results.Success, "Compilation Failed")
    val cname = codeName(code)
    if (result == Results.Success) {
      w.getBuffer.delete(0, w.getBuffer.length())
      val anyVar = im.mostRecentVar
      val res = im.interpret("val " + cname + ": HTML = " + anyVar)
      assert(res == Results.Success, "failed to set value for " + cname + ": " + res + "\n" + w.getBuffer.toString)
      //im.interpret(cname)
      //println(im.definedTerms.mkString(", "))
      //println(im.mostRecentVar)
      //println(im.valueOfTerm(im.mostRecentVar))
      //val llog = w.getBuffer.toString
      //w.getBuffer.delete(0, w.getBuffer.length())
      //assert(im.valueOfTerm(cname).isDefined, s"Recent var: ${im.mostRecentVar}\n${cname}: ${im.valueOfTerm(cname)}\n$llog")
      val lres = im.bind(cname + "Log", "String", log)
      assert(lres == Results.Success, "failed to set value for " + cname + "Log: " + lres + "\n" + w.getBuffer.toString)
      //assert(checkCodeCompiled(im, code), s"Code not compiled:\n$code")
      //println("gv: " + getValue(im, w, code))
    } else {
      if (log.trim.isEmpty) throw new Exception("<pre class=\"error\">Could not compile</pre>")
      else {
        val errString = log
        throw new Exception("<pre class=\"error\">" + errString + "</pre>") //.replaceAll("\n", "</pre><pre>") + "</pre>")
      }
      //assert(false, "Should not be here")
    }
  }

  def valueOfTerm(im: IMain, w: StringWriter, name: String): String = {
    w.getBuffer.delete(0, w.getBuffer.length())
    im.interpret(name)
    val log = w.getBuffer.toString
    log
  }

  def getValue(im: IMain, w: StringWriter, code: String): Result = {
    val cname = codeName(code)
    val html = valueOfTerm(im, w, cname + ".source").replaceFirst("res\\d+: String =", "").trim.replaceFirst("^\"", "").replaceFirst("\"$", "").trim
    //println("---- HTML ----")
    //println(html)
    //assert(html.isDefined)
    val log = valueOfTerm(im, w, cname + "Log").replaceFirst("res\\d+: String =", "").trim
    //println("---- Log ----")
    //println(log)
    //assert(log.isDefined, s"Cannot find value of " + cname + "Log")
    //if (html.isDefined)
    Result(html, log) //.get.asInstanceOf[HTML].source, .get.asInstanceOf[String]
    //else Result("<b>Compile Error!</b>", log) //.get.asInstanceOf[String]
  }

  def execute(sessionId: String, snippets: Array[String]): Result = {
    val (im, w) = imainPair(sessionId)
    // find first changed snippet
    val firstChanged = snippets.toSeq.indexWhere(code => !checkCodeCompiled(im, code))
    println(s"First Changed Index ${firstChanged + 1} of ${snippets.size}")
    // compile and run all code after, keeping track of last
    if (firstChanged >= 0) {
      println("First Changed: " + snippets(firstChanged))
      for (idx <- firstChanged until snippets.length) {
        val code = snippets(idx)
        interpret(im, w, code)
      }
    }
    getValue(im, w, snippets.last)
  }

  override def autocomplete(sessionId: String, prefix: String): Seq[String] = {
    // From: http://stackoverflow.com/a/20333972
    val im = imain(sessionId)
    val comp = new JLineCompletion(im)
    val completer = comp.completer()
    val Candidates(_, choices) = completer.complete(prefix, prefix.length)
    choices
  }

  override def autocompleteLine(sessionId: String, prefix: String): Seq[String] = {
    // From: http://stackoverflow.com/a/20333972
    import scala.collection.JavaConversions._
    //import scala.tools.jline.console.completer._
    def scalaToJline(tc: ScalaCompleter): Completer = new Completer {
      def complete(_buf: String, cursor: Int, candidates: JList[CharSequence]): Int = {
        val buf = if (_buf == null) "" else _buf
        val Candidates(newCursor, newCandidates) = tc.complete(buf, cursor)
        newCandidates foreach (candidates add _)
        newCursor
      }
    }

    val im = imain(sessionId)
    val comp = new JLineCompletion(im)
    val completer = comp.completer()
    val argCompletor: ArgumentCompleter = new ArgumentCompleter(new JLineDelimiter, scalaToJline(comp.completer))
    argCompletor.setStrict(false)
    val maybes = new java.util.ArrayList[CharSequence]
    argCompletor.complete(prefix, prefix.length, maybes)
    maybes.toSeq.map(_.toString)
  }

  override def compile(sessionId: String, codes: Array[String]): Result = execute(sessionId, codes)
}
