package controllers

/*
 * Copyright 2010 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.sameersingh.htmlgen.HTML
import string._

import java.io._
import java.math.BigInteger
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.Random
import java.util.jar.JarFile
import scala.collection.mutable
import scala.io.Source
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.util.AbstractFileClassLoader
import scala.tools.nsc.io.{AbstractFile, VirtualDirectory}
import scala.tools.nsc.reporters.AbstractReporter
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.internal.util.Position
import scala.util.matching.Regex

/**
 * Evaluate a file or string and return the result.
 */
object Evaluator {
  private val jvmId = java.lang.Math.abs(new Random().nextInt())
  val classCleaner: Regex = "\\W".r
}

/**
 * Evaluates files, strings, or input streams as Scala code, and returns the result.
 *
 * If `target` is `None`, the results are compiled to memory (and are therefore ephemeral). If
 * `target` is `Some(path)`, the path must point to a directory, and classes will be saved into
 * that directory.
 *
 * Eval also supports a limited set of preprocessors. Currently, "limited" means "exactly one":
 * directives of the form `#include <file>`.
 *
 * The flow of evaluation is:
 * - extract a string of code from the file, string, or input stream
 * - run preprocessors on that string
 * - wrap processed code in an `apply` method in a generated class
 * - compile the class
 * - contruct an instance of that class
 * - return the result of `apply()`
 */
class Evaluator(target: Option[File] = None, classPath: List[String] = List.empty,
                imports: List[String] = List.empty,
                classesForJarPath: List[String] = List.empty,
                multipleObjs: Boolean = true) extends ScalaInterpreter {

  import Evaluator.jvmId

  private lazy val compilerPath = try {
    jarPathOfClass("scala.tools.nsc.Interpreter")
  } catch {
    case e: Throwable =>
      throw new RuntimeException("Unable lo load scala interpreter from classpath (scala-compiler jar is missing?)", e)
  }

  private lazy val libPath = try {
    jarPathOfClass("scala.Any")
  } catch {
    case e: Throwable =>
      throw new RuntimeException("Unable to load scala base object from classpath (scala-library jar is missing?)", e)
  }

  private lazy val additionalPath =
    classesForJarPath.map(c =>
      try {
        jarPathOfClass(c)
      } catch {
        case e: Throwable =>
          throw new RuntimeException("Unable to load custom class from classpath (relevant jar for " + c + " missing?)", e)
      }
    ).flatten.toList

  private[this] val STYLE_INDENT = 2
  private[this] lazy val compiler = new StringCompiler(STYLE_INDENT, target)

  def getClassAndName(codes: Array[String]): (String, Class[_]) = {
    assert(codes.length > 0)
    if (multipleObjs) {
      if (codes.length == 1) {
        val id = uniqueId(codes.head)
        val className = "Moro__" + id
        val wrappedCode = wrapCodeInClass(className, codes.head, true)
        println("code: " + wrappedCode)
        val cls = compiler(wrappedCode, className + "$", false)
        (className, cls)
      } else {
        val (parentClassName, parentClassObj) = getClassAndName(codes.dropRight(1))
        val code = "import " + parentClassName + "._\n\n" + codes.last
        val id = uniqueId(code)
        val className = "Moro__" + id
        val wrappedCode = wrapCodeInClass(className, code, true)
        println("code: " + wrappedCode)
        val cls = compiler(wrappedCode, className + "$", false)
        (className, cls)
      }
    } else {
      val combinedCode = codes.mkString("\n")
      val id = uniqueId(combinedCode)
      val className = "Moro__" + id
      val wrappedCode = wrapCodeInClass(className, combinedCode, true)
      println("wrapped code: " + wrappedCode)
      val cls = compiler(wrappedCode, className + "$", false)
      (className, cls)
    }
  }

  override def compile(sessionId: String, codes: Array[String]): Result = Result(applyProcessed[org.sameersingh.htmlgen.HTML](codes).source)

  def applyProcessed[T](codes: Array[String]): T = {
    val (clsName, cls) = getClassAndName(codes)
    val objectRef = cls.getField("MODULE$").get(null)
    objectRef.asInstanceOf[() => Any].apply().asInstanceOf[T]
  }

  /**
   * val i: Int = new Eval()("1 + 1") // => 2
   */
  def apply[T](code: String, resetState: Boolean = true): T = {
    applyProcessed(code, resetState)
  }

  /**
   * same as apply[T], but does not run preprocessors.
   * Will generate a classname of the form Evaluater__<unique>,
   * where unique is computed from the jvmID (a random number)
   * and a digest of code
   */
  def applyProcessed[T](code: String, resetState: Boolean): T = {
    val id = uniqueId(code)
    val className = "Moro__" + id
    applyProcessed(className, code, resetState)
  }

  /**
   * same as apply[T], but does not run preprocessors.
   */
  def applyProcessed[T](className: String, code: String, resetState: Boolean): T = {
    val wrappedCode = wrapCodeInClass(className, code)
    println("code: " + wrappedCode)
    val cls = compiler(wrappedCode, className + "$", resetState)
    //cls.getConstructor().newInstance().asInstanceOf[() => Any].apply().asInstanceOf[T]
    val objectRef = cls.getField("MODULE$").get(null)
    objectRef.asInstanceOf[() => Any].apply().asInstanceOf[T]
  }

  private def uniqueId(code: String, idOpt: Option[Int] = Some(jvmId)): String = {
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
  private def wrapCodeInClass(className: String, code: String, repeat: Boolean = true) = {
    "object " + className + " extends (() => Any) {\n" +
      imports.map(i => "import " + i + "\n").mkString("") +
      (if (repeat) code else "") + "\n" +
      "  def apply(): org.sameersingh.htmlgen.HTML = {\n" +
      code + "\n" +
      "  }\n" +
      "}\n"
  }

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

  /*
   * Try to guess our app's classpath.
   * This is probably fragile.
   */
  lazy val impliedClassPath: List[String] = {
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

  trait Preprocessor {
    def apply(code: String): String
  }

  /**
   * Dynamic scala compiler. Lots of (slow) state is created, so it may be advantageous to keep
   * around one of these and reuse it.
   */
  private class StringCompiler(lineOffset: Int, targetDir: Option[File]) {
    val target = targetDir match {
      case Some(dir) => AbstractFile.getDirectory(dir)
      case None => new VirtualDirectory("(memory)", None)
    }

    val cache = new mutable.HashMap[String, Class[_]]()

    val settings = new Settings
    settings.nowarnings.value = true // warnings are exceptions, so disable
    settings.outputDirs.setSingleOutput(target)
    val pathList = compilerPath ::: libPath ::: additionalPath
    settings.bootclasspath.value = pathList.mkString(File.pathSeparator)
    settings.classpath.value = (pathList ::: impliedClassPath).mkString(File.pathSeparator)
    println(settings.classpath.value)

    val reporter = new AbstractReporter {

      val settings = StringCompiler.this.settings
      val messages = new mutable.ListBuffer[Tuple3[Position, String, Int]]

      def display(pos: Position, message: String, severity: Severity) {
        severity.count += 1
        val severityName = severity match {
          case ERROR => "error: "
          case WARNING => "warning: "
          case _ => ""
        }

        messages += Tuple3(pos, message, severity.id)
      }

      def displayPrompt {
        // no.
      }

      override def reset {
        super.reset
        messages.clear()
      }
    }

    val global = new Global(settings, reporter)

    /*
     * Class loader for finding classes compiled by this StringCompiler.
     * After each reset, this class loader will not be able to find old compiled classes.
     */
    val parentClassLoader = new URLClassLoader(impliedClassPath.map(p => new File(p).toURI.toURL).toArray, this.getClass.getClassLoader) {
      println(impliedClassPath.mkString("\t"))
    }
    var classLoader = new AbstractFileClassLoader(target, parentClassLoader)

    def reset() {
      targetDir match {
        case None => {
          target.asInstanceOf[VirtualDirectory].clear
        }
        case Some(t) => {
          target.foreach {
            abstractFile =>
              if (abstractFile.file == null || abstractFile.file.getName.endsWith(".class")) {
                abstractFile.delete
              }
          }
        }
      }
      cache.clear()
      reporter.reset
      val parentClassLoader = new URLClassLoader(pathList.map(p => new File(p).toURI.toURL).toArray, this.getClass.getClassLoader)
      classLoader = new AbstractFileClassLoader(target, parentClassLoader)
    }

    object Debug {
      val enabled =
        System.getProperty("eval.debug") != null

      def printWithLineNumbers(code: String) {
        printf("Code follows (%d bytes)\n", code.length)

        var numLines = 0
        code.lines foreach {
          line: String =>
            numLines += 1
            println(numLines.toString.padTo(5, ' ') + "| " + line)
        }
      }
    }

    def findClass(className: String): Option[Class[_]] = {
      synchronized {
        cache.get(className).orElse {
          try {
            val cls = classLoader.loadClass(className)
            cache(className) = cls
            Some(cls)
          } catch {
            case e: ClassNotFoundException => None
          }
        }
      }
    }

    /**
     * Compile scala code. It can be found using the above class loader.
     */
    def apply(code: String) {
      if (Debug.enabled)
        Debug.printWithLineNumbers(code)

      // if you're looking for the performance hit, it's 1/2 this line...
      val compiler = new global.Run
      val sourceFiles = List(new BatchSourceFile("(inline)", code))
      // ...and 1/2 this line:
      compiler.compileSources(sourceFiles)

      if (reporter.hasErrors || reporter.WARNING.count > 0) {
        throw new CompilerException(reporter.messages.toList)


      }
    }

    /**
     * Compile a new class, load it, and return it. Thread-safe.
     */
    def apply(code: String, className: String, resetState: Boolean = true): Class[_] = {
      synchronized {
        if (resetState) reset()
        findClass(className).getOrElse {
          apply(code)
          findClass(className).get
        }
      }
    }
  }

}

case class CompilerException(m: List[(Position, String, Int)]) extends Exception("Compiler exception")
