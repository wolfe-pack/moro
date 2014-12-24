package controllers

import controllers.util.MoroConfig
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * @author sameer
 * @since 12/24/14.
 */
/**
 * Scala server using the Twitter Eval implementation
 */
class ScalaServer(c: MoroConfig) extends Compiler with ACEEditor {

  def name = "scala"

  override val config = c.config(this)
  val classPath = config.map(c => c.getStringList("classPath")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)
  val classesForJarPath = config.map(c => c.getStringList("classesForJarPath")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)
  val imports = config.map(c => c.getStringList("imports")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)

  // aggregate all the previous cells as well?
  override val aggregatePrevious: Boolean = config.map(c => c.getBoolean("aggregate").getOrElse(false)).getOrElse(false)

  // whether to hide the editor after compilation or not (essentially replacing editor with the output)
  override def hideAfterCompile: Boolean = false

  // Support for caching of previous classes..
  val numCompiledClasses = 10
  val compiledMap = new mutable.HashMap[String,Any]


  def compile(input: Input) = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    val code = input.code;
    val eval = new Evaluator(None, classPath, imports, classesForJarPath)
    println("compiling code : " + code)
    val result = try {
      eval.apply[org.sameersingh.htmlgen.HTML](code, false).source
    } catch {
      case e: CompilerException => {
        e.printStackTrace()
        //"<span class=\"label label-danger\">Error on line %d, col %d: %s</span>" format(e.m.head._1.line - 4, e.m.head._1.column - 4, e.m.head._2)
        "<span class=\"label label-danger\">Error: %s</span>" format(e.m.head._2)

      }
    } finally {
      "Compile Error!!"
    }
    println("result: " + result)
    Result("<div class=\"string-result\">" + "<blockquote>" + result.toString + "</blockquote>" + "</div>")
  }
}

