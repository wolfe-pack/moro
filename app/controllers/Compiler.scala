package controllers

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{IMain, ILoop}
import java.io.File
import play.api.Configuration
import controllers.util.MoroConfig
import scala.collection.JavaConverters._
/**
 * @author sameer
 */

object OutputFormats extends Enumeration {
  val string, html, javascript = Value

  implicit def outputFormatToString(f: Value): String = f.toString

  implicit def stringToOutputFormat(s: String): Value = withName(s)
}

import OutputFormats._

case class Input(code: String, outputFormat: String = OutputFormats.html, extraFields: Map[String, String] = Map.empty)

case class Result(result: String, format: String = OutputFormats.html)

/**
 * A Wolfenstein Compiler
 */
trait Compiler {
  // name of the compiler that should be unique in a collection of compilers
  def name: String

  // code to construct the editor for a cell of this type
  def editorJavascript: String

  // code to construct the editor for a cell of this type
  def removeEditorJavascript: String

  // whether to hide the editor after compilation or not (essentially replacing editor with the output)
  def hideAfterCompile: Boolean = true

  // code that compiles an input to output, in Scala
  def compile(input: Input): Result

  // javascript that extracts the code from the editor and creates a default input
  def editorToInput: String

  // icon that is used in the toolbar
  def toolbarIcon: String = name(0).toUpper + name.drop(1)

  def start = {}
}

/**
 * Compiler where the editor is a basic ACE editor
 */
trait ACEEditor {
  this: Compiler =>
  def editorMode: String = name

  def outputFormat: OutputFormats.Value = OutputFormats.html

  def initialValue: String = ""

  def editorJavascript: String =
    """
      |function(id,content) {
      |    $("#editor"+id).empty();
      |    $("#editor"+id).height("auto");
      |    var editor = ace.edit("editor"+id);
      |    editor.setTheme("ace/theme/solarized_light");
      |    editor.getSession().setMode("ace/mode/%s");
      |    var contentToAdd = ""
      |    if(content=="") contentToAdd = '%s';
      |    else contentToAdd = content;
      |    editor.getSession().setValue(contentToAdd);
      |    editor.focus();
      |    editor.navigateFileEnd();
      |    editor.setBehavioursEnabled(false);
      |
      |    heightUpdateFunction(editor, '#editor'+id);
      |    editor.getSession().on('change', function () {
      |        heightUpdateFunction(editor, '#editor'+id);
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

/**
 * Compiler where the editor is a basic HTML TextInput
 */
trait TextInputEditor {
  this: Compiler =>
  def outputFormat: OutputFormats.Value = OutputFormats.html

  def fieldLabel: String

  def initialValue: String = ""

  def editorJavascript: String =
    """
      |function(id,content) {
      |    var contentToAdd = ""
      |    if(content=="") contentToAdd = '%s';
      |    else contentToAdd = content;
      |    $("#editor"+id).empty();
      |    $("#editor"+id).height("auto");
      |    $("#editor"+id).html(
      |      '<div class="input-group">' +
      |      '  <span class="input-group-addon">%s</span>' +
      |      '  <input id="editorInput'+id+'" type="text" class="form-control" placeholder="%s" value="'+contentToAdd+'">' +
      |      '</div>');
      |    $("#editorInput"+id).focus();
      |
      |    return $("#editorInput"+id);
      |}
    """.stripMargin format (initialValue, fieldLabel, initialValue)

  // code to construct the editor for a cell of this type
  def removeEditorJavascript: String =
    """
      |function(id) {
      |  $("#editor"+id).empty();
      |}
    """.stripMargin

  // javascript that extracts the code from the editor and creates a default input
  def editorToInput: String =
    """
      |function (doc, id) {
      |  input = {}
      |  input.code = doc.cells[id].editor.val();
      |  input.outputFormat = "%s";
      |  return input;
      |};
    """.stripMargin format (outputFormat)
}

/* --------------------------------------------------
 *                   COMPILERS
 * --------------------------------------------------
 */
class HTMLCompiler extends Compiler with ACEEditor {
  def name: String = "html"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "&lt;html&gt;"

  def compile(input: Input): Result = {
    assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result(input.code, outputFormat)
  }
}

class HeadingCompiler(val level: Int) extends Compiler with TextInputEditor {
  def name: String = "heading" + level

  def fieldLabel: String = "Heading"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<span class=\"glyphicon glyphicon-header\">%d</span>" format (level)

  def compile(input: Input): Result = {
    assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result("<h%d>%s</h%d>" format(level, input.code, level), outputFormat)
  }
}

class ImageURLCompiler extends Compiler with TextInputEditor {
  def name: String = "imageurl"

  def fieldLabel: String = "url"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<i class=\"fa fa-picture-o\"></i>"

  def compile(input: Input): Result = {
    assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result("<img src=\"%s\" class=\"img-thumbnail displayed\" />" format (input.code), outputFormat)
  }
}

/**
 * Basic markdown compiler using Actuarius
 */
class ActuriusCompiler extends Compiler with ACEEditor {

  import eu.henkelmann.actuarius.ActuariusTransformer

  def name = "markdown"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "&Mu;d"

  val transformer = new ActuariusTransformer()

  def compile(input: Input) = {
    assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result(transformer(input.code), outputFormat)
  }
}

class LatexCompiler extends Compiler with ACEEditor {
  def name = "latex"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<i class=\"fa fa-superscript\"></i>"

  def compile(input: Input) = {
    assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result("$$" + input.code + "$$", outputFormat)
  }

}

/**
 * Scala server using the Twitter Eval implementation
 */
class TwitterEvalServer(c: MoroConfig) extends Compiler with ACEEditor {

  def name = "scala"

  val config = c.config(this)
  val classPath = config.map(c => c.getStringList("classPath")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)
  val classesForJarPath = config.map(c => c.getStringList("classesForJarPath")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)
  val imports = config.map(c => c.getStringList("imports")).getOrElse(None).map(l => l.asScala.toList).getOrElse(List.empty)

  override def outputFormat: OutputFormats.Value = OutputFormats.string

  // whether to hide the editor after compilation or not (essentially replacing editor with the output)
  override def hideAfterCompile: Boolean = false

  def compile(input: Input) = {
    assert(input.outputFormat equalsIgnoreCase outputFormat)
    val code = input.code;
    val eval = new Evaluator(None, classPath, imports, classesForJarPath)
    println("compiling code : " + code)
    val result = try {
      eval.apply[Any](code, false)
    } catch {
      case e: CompilerException => e.m.mkString("\n\t")
    } finally {
      "Compile Error!!"
    }
    println("result: " + result)
    Result(result.toString, outputFormat)
  }
}

class GoogleDocsViewer extends Compiler with TextInputEditor {
  // name of the compiler that should be unique in a collection of compilers
  def name: String = "google_viewer"

  def fieldLabel: String = "url"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<i class=\"fa fa-eye\"></i>"

  def compile(input: Input) = {
    assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result(
      "<iframe src=\"http://docs.google.com/viewer?url=" + input.code + "&embedded=true\" style=\"width:800px; height:600px;\" frameborder=\"0\"></iframe>",
      outputFormat)
  }
}