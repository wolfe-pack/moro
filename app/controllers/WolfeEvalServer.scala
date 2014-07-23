package controllers

import controllers.util.MoroConfig
import collection.JavaConverters._

/**
 * @author Sebastian Riedel
 */
/**
 * Scala server using the Twitter Eval implementation
 */
class WolfeEvalServer(c: MoroConfig) extends Compiler with WolfeACEEditor {

  def name = "wolfe"

  val config = c.config(this)
  val classPath = config.map(c => c.getStringList("classPath")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)
  val classesForJarPath = config.map(c => c.getStringList("classesForJarPath")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)
  val imports = config.map(c => c.getStringList("imports")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)

  // aggregate all the previous cells as well?
  override val aggregatePrevious: Boolean = config.map(c => c.getBoolean("aggregate").getOrElse(false)).getOrElse(false)

  override def editorMode = "scala"

  // whether to hide the editor after compilation or not (essentially replacing editor with the output)
  override def hideAfterCompile: Boolean = false

  val userHome = System.getProperty("user.home") + "/" //"/Users/sriedel/"

  def compile(input: Input) = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    val code = input.code;
    val eval = new Evaluator(None, classPath ++ List(
      userHome + ".ivy2/cache/ml.wolfe/wolfe-core_2.10/jars/wolfe-core_2.10-0.1.0-SNAPSHOT.jar",
      userHome + ".ivy2/local/ml.wolfe/wolfe-core_2.10/0.1.0-SNAPSHOT/jars/wolfe-core_2.10.jar",
      userHome + ".ivy2/local/ml.wolfe/wolfe-examples_2.10/0.1.0-SNAPSHOT/jars/wolfe-examples_2.10.jar",
      userHome + ".ivy2/cache/net.sf.trove4j/trove4j/jars/trove4j-3.0.3.jar",
      userHome + ".ivy2/cache/com.typesafe/scalalogging-slf4j_2.10/jars/scalalogging-slf4j_2.10-1.1.0.jar",
      userHome + ".ivy2/cache/org.slf4j/slf4j-api/jars/slf4j-api-1.7.6.jar",
      userHome + ".ivy2/cache/org.slf4j/slf4j-simple/jars/slf4j-simple-1.7.6.jar",
      userHome + ".ivy2/cache/org.scala-lang/scala-reflect/jars/scala-reflect-2.10.3.jar",
      userHome + ".ivy2/cache/cc.factorie/factorie/jars/factorie-1.0.0-M7.jar"), imports, classesForJarPath
    )
    println("compiling code : " + code)
    val result = try {
      eval.apply[org.sameersingh.htmlgen.HTML](code, false).source
    } catch {
      case e: CompilerException => e.m.mkString("\n\t")
    } finally {
      "Compile Error!!"
    }
    println("result: " + result)
    Result("<blockquote>" + result.toString+"</blockquote>", outputFormat)
  }
}

/**
 * Compiler where the editor is a basic ACE editor
 */
trait WolfeACEEditor {
  this: Compiler =>
  def editorMode: String = name

  def outputFormat: OutputFormats.Value = OutputFormats.html

  def initialValue: String = ""

  def editorJavascript: String =
    """
      |function(id,content) {
      |    var editor = ace.edit("editor"+id);
      |    editor.setTheme("ace/theme/wolfe");
      |    editor.getSession().setMode("ace/mode/%s");
      |    var contentToAdd = ""
      |    if(content=="") contentToAdd = '%s';
      |    else contentToAdd = content;
      |    editor.getSession().setValue(contentToAdd);
      |    editor.focus();
      |    editor.navigateFileEnd();
      |    editor.setBehavioursEnabled(false);
      |    editor.setHighlightActiveLine(false);
      |    editor.setShowPrintMargin(false);
      |    wolfeHeightUpdateFunction(editor, '#editor'+id);
      |    editor.getSession().on('change', function () {
      |        wolfeHeightUpdateFunction(editor, '#editor'+id);
      |    });
      |
      |    editor.commands.addCommand({
      |        name: "runCode",
      |        bindKey: {win: "Ctrl-Enter", mac: "Ctrl-Enter"},
      |        exec: function(editor) {
      |            document.getElementById("runCode"+id).click();
      |        }
      |    })
      |    return editor;
      |}
    """.stripMargin format (editorMode, initialValue)

  // code to construct the editor for a cell of this type
  def removeEditorJavascript: String =
    """
      |function(id) {
      |  var editor = doc.cells[id].editor
      |  var value = editor.getValue()
      |  editor.destroy()
      |  var oldDiv = editor.container
      |  var newDiv = oldDiv.cloneNode(false)
      |  newDiv.textContent = value
      |  oldDiv.parentNode.replaceChild(newDiv, oldDiv)
      |}
    """.stripMargin

  // javascript that extracts the code from the editor and creates a default input
  def editorToInput: String =
    """
      |function (doc, id) {
      |  input = {}
      |  input.code = doc.cells[id].editor.getSession().getValue();
      |  input.outputFormat = "%s";
      |  return input;
      |};
    """.stripMargin format (outputFormat)
}

