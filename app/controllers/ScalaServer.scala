package controllers

import java.io.File

import controllers.util.MoroConfig
import play.api.libs.json.Json
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
  println("cp: " + config.get.getStringList("classPath"))
  println("docRoot: " + config.get.getStringList("classPath"))

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

  val interpreter: ScalaInterpreter =
    //new Evaluator(None, classPath, imports, classesForJarPath, false) //Some(new File("runtime-classes")
    new ScalaIMainInterpreter(None, classPath, imports, classesForJarPath)

  def compile(input: Input) = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    val aggregatedCells = Json.fromJson[Array[String]](Json.parse(input.extraFields("aggregatedCells"))).get
    //println(aggregatedCells.mkString("{", "}, {", "}"))
    val code = input.code
    //println(classPath.mkString("\t"))
    val result = try {
      interpreter.compile(input.sessionId, aggregatedCells ++ Array(code))
    } catch {
      case e: CompilerException => {
        e.printStackTrace()
        //"<span class=\"label label-danger\">Error on line %d, col %d: %s</span>" format(e.m.head._1.line - 4, e.m.head._1.column - 4, e.m.head._2)
        Result("<span class=\"label label-danger\">Compile Error!\n%s\n</span>" format(e.m.head._2))

      }
      case e: Exception => {
        e.printStackTrace()
        Result("<span class=\"label label-danger\">Error!</span>\n%s" format(e.getMessage))
      }
    } finally {
        Result("Compile Error!!")
    }
    //println("result: " + result)
    Result("<div class=\"string-result\">" + result.result + "</div>", result.log)
  }
}

trait ScalaInterpreter {
  def compile(sessionId: String, codes: Array[String]): Result
  def autocomplete(sessionId: String, prefix: String): Seq[String] = Seq.empty
  def autocompleteLine(sessionId: String, prefix: String): Seq[String] = Seq.empty
}