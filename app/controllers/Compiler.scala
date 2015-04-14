package controllers

import controllers.doc.Document
import play.api.libs.json.Json

import scala.StringBuilder
import scala.collection.mutable
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{IMain, ILoop}
import java.io.{FileWriter, File}
import play.api.Configuration
import controllers.util.{Cache, MoroConfig}

/**
 * @author sameer
 */

object OutputFormats extends Enumeration {
  val string, html, javascript = Value

  implicit def outputFormatToString(f: Value): String = f.toString

  implicit def stringToOutputFormat(s: String): Value = withName(s)
}

import OutputFormats._

case class Input(sessionId: String, code: String, extraFields: Map[String, String] = Map.empty, outputFormat: Option[String] = None) {
  def config: Map[String, String] = if (extraFields == null) Map.empty else extraFields

  def configJson = Json.stringify(Json.toJson(config))
}

case class Result(result: String, log: String = "")

/**
 * A Description of a configuration element
 * @param key a short string that uniquely defines this element (no spaces etc., should follow HTML id rules)
 * @param label a short name of the configuration that the user sees
 * @param description a longer (sentence long) description of the configuration, including value domains, etc.
 *                    NOTE: for "select" type, this is a tab-separated list of values
 * @param inputType an HTML input type, such as checkbox, text, password, email, file, ... or "select"
 * @param defaultValue default value that the configuration should have
 */
case class ConfigEntry(key: String, label: String, description: String, inputType: String, defaultValue: String)

object CompilerConfigKeys {
  val Hide = "hide"
  val HideOutput = "hide_output"
  val ShowEditor = "showEditor"
  val CacheResults = "cache"
  val Aggregate = "aggregate"
  val Scope = "scope"
  val Fragment = "fragment"
}

/**
 * A Moro Compiler
 */
trait Compiler {
  // name of the compiler that should be unique in a collection of compilers
  def name: String

  def config: Option[Configuration] = None

  // code to construct the editor for a cell of this type
  def editorJavascript(inEditorView:Boolean = true): String

  // code to construct the editor for a cell of this type
  def removeEditorJavascript: String

  // whether to hide the editor after compilation or not (essentially replacing editor with the output)
  def hideAfterCompile: Boolean = true

  // code that compiles an input to output, in Scala
  def compile(input: Input): Result

  def process(input: Input): Result = compile(input)

  // javascript that extracts the code from the editor and creates a default input
  def editorToInput: String

  // icon that is used in the toolbar
  def toolbarIcon: String = name(0).toUpper + name.drop(1)

  // aggregate all the previous cells as well?
  def aggregatePrevious: Boolean = false

  // HTML imports that are added before the cells, needed to run cells of this type
  def prefixHTML: String = ""

  import CompilerConfigKeys._

  def configEntries: Seq[ConfigEntry] = Seq(
    ConfigEntry(Hide, "Hide Cell?", "Hide this cell in static/presentation views.", "checkbox", "false"),
    ConfigEntry(HideOutput, "Hide Cell Output?", "Hide this cell's output.", "checkbox", "false"),
    ConfigEntry(ShowEditor, "Show Editor?", "Whether to show the editor of this cell around in static/presentation views.", "checkbox", "false"),
    ConfigEntry(CacheResults, "Cached", "Use cached results, uncheck if running again should produce different results.", "checkbox", "true"),
    ConfigEntry(Aggregate, "Aggregate", "If compiler allows, aggregate inputs across cells of the same type (and scope).", "checkbox", "true"),
    ConfigEntry(Scope, "Scope", "Scope use when aggregating cells (not used otherwise).", "text", "_default"),
    ConfigEntry(Fragment, "Fragment", "If checked, the presentation mode pauses before displaying this cell.", "checkbox", "false")
  )

  implicit val ceWrites = Json.writes[ConfigEntry]

  def configEntriesJson: String = Json.stringify(Json.toJson(configEntries))

  def start = {}
}

/**
 * Compiler where there is no editor, i.e. no input
 */
trait NoEditor {
  this: Compiler =>
  //def outputFormat: OutputFormats.Value = OutputFormats.html
  def description: String

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
      |      '  <input id="editorInput'+id+'" type="text" class="form-control" disabled="true" placeholder= "%s" value="'+contentToAdd+'">' +
      |      '</div>');
      |    $("#editorInput"+id).focus();
      |
      |    return $("#editorInput"+id);
      |}
    """.stripMargin format(description, description)

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
      |  input.code = '';
      |  return input;
      |};
    """.stripMargin

}

/**
 * Compiler where the editor is a basic ACE editor
 */
trait ACEEditor {
  this: Compiler =>
  def editorMode: String = name

  // def outputFormat: OutputFormats.Value = OutputFormats.html

  def initialValue: String = ""

  def aceTheme: String = "tomorrow"

  def editorJavascript(inEditorView:Boolean = true): String =
    """
      |function(id,content) {
      |    $("#editor"+id).empty();
      |    var editor = ace.edit("editor"+id);
      |    editor.setOptions({
      |      maxLines: Infinity,
      |      enableBasicAutocompletion: true,
      |      enableSnippets: true,
      |      enableLiveAutocompletion: true
      |    });
      |    editor.setTheme("ace/theme/%s");
      |    editor.getSession().setMode("ace/mode/%s");
      |    editor.getSession().setUseWrapMode(true);
      |    editor.getSession().setWrapLimitRange(null, null);
      |    var contentToAdd = ""
      |    if(content=="") contentToAdd = '%s';
      |    else contentToAdd = content;
      |    //editor.renderer.setScrollMargin(10, 10, 10, 10);
      |    editor.getSession().setValue(contentToAdd, 1);
      |    editor.focus();
      |    editor.navigateFileEnd();
      |    editor.setBehavioursEnabled(true);
      |    editor.setWrapBehavioursEnabled(true);
      |    editor.setShowFoldWidgets(true);
      |    editor.setHighlightActiveLine(false);
      |    editor.setShowPrintMargin(false);
      |    editor.setAutoScrollEditorIntoView(true);
      |    editor.renderer.setShowGutter(true);
      |    editor.on('change', function () {
      |        //heightUpdateFunction(editor, '#editor'+id);
      |    });
      |
      |    editor.commands.addCommand({
      |        name: "runCode",
      |        bindKey: {win: "Ctrl-Enter", mac: "Ctrl-Enter"},
      |        exec: function(editor) {
      |            document.getElementById("runCode"+id).click();
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "addCellBelow",
      |        bindKey: {win: "Shift-Enter", mac: "Shift-Enter"},
      |        exec: function(editor) {
      |            document.getElementById('addBelow' + id).click();
      |            ne=nextEditor(doc,id);
      |            oldMode=currentMode(id);
      |            if(typeof(ne)!='undefined') {
      |              editor.getSelection().selectFileEnd();
      |              var text = editor.getCopyText();
      |              editor.remove('right');
      |              console.log("splitting a cell!");
      |              ne.focus();
      |              ne.insert(text,1);
      |              ne.navigateTo(0, 0);
      |              newId = nextCellId(id);
      |              changeMode(newId,oldMode)
      |              console.log("changing " + newId + " to " + oldMode);
      |            }
      |            if (oldMode == 'markdown') document.getElementById('runCode' + id).click();
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "injectEmptyCell",
      |        bindKey: {win: "Shift-Ctrl-Enter", mac: "Shift-Ctrl-Enter"},
      |        exec: function(editor) {
      |            document.getElementById('addBelow' + id).click();
      |            ne=nextEditor(doc,id);
      |            oldMode=currentMode(id);
      |            if(typeof(ne)!='undefined') {
      |              editor.getSelection().selectFileEnd();
      |              var text = editor.getCopyText();
      |              editor.remove('right');
      |              console.log("splitting a cell!");
      |              ne.insert(text,1);
      |            }
      |            if (oldMode == 'markdown') document.getElementById('runCode' + id).click();
      |            if (!(ne.getValue().trim() === "")) document.getElementById('addBelow' + id).click();
      |            ne=nextEditor(doc,id);
      |            newId = nextCellId(id);
      |            changeMode(newId,toggledMode(oldMode));
      |            if(typeof(ne)!='undefined') {
      |              ne.focus();
      |              ne.navigateTo(0, 0);
      |            }
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "deleteCell",
      |        bindKey: {win: "Shift-Del", mac: "Shift-Del"},
      |        exec: function(editor) {
      |            document.getElementById('remove' + id).click();
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "moveCursorDown",
      |        bindKey: {win: 'Down', mac: 'Down'},
      |        exec: function(editor) {
      |            if(editor.getCursorPosition().row + 1 == editor.getSession().getDocument().getLength()) {
      |              ne=nextEditor(doc,id);
      |              if(typeof(ne)!='undefined') {
      |                ne.navigateTo(0, editor.getCursorPosition().column);
      |                ne.focus();
      |              }
      |            }
      |            editor.navigateDown(1);
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "moveCursorUp",
      |        bindKey: {win: 'Up', mac: 'Up'},
      |        exec: function(editor) {
      |            if(editor.getCursorPosition().row == 0) {
      |              pe=prevEditor(doc,id);
      |              if(typeof(pe)!='undefined') {
      |                pe.navigateTo(pe.getSession().getDocument().getLength()-1, editor.getCursorPosition().column);
      |                pe.focus();
      |              }
      |            }
      |            editor.navigateUp(1);
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "mergeBefore",
      |        bindKey: {win: "Shift-Backspace|Backspace", mac: "Ctrl-Backspace|Shift-Backspace|Backspace|Ctrl-H"},
      |        exec: function(editor) {
      |            var curPos = editor.getCursorPosition();
      |            if(curPos.row == 0 && curPos.column == 0) {
      |              pe=prevEditor(doc,id);
      |              if(typeof(pe)!='undefined') {
      |                console.log("merge with previous!");
      |                var text = editor.getSession().getDocument().getValue();
      |                pe.focus();
      |                pe.gotoLine(pe.getSession().getDocument().getLength());
      |                pe.navigateLineEnd();
      |                var newCurPos = pe.getCursorPosition();
      |                pe.insert('\n'+ text);
      |                pe.navigateTo(newCurPos.row+1, 0);
      |                document.getElementById('remove' + id).click();
      |              }
      |            } else {
      |              editor.remove("left");
      |            }
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "mergeAfter",
      |        bindKey: {win: "Delete", mac: "Delete|Ctrl-D|Shift-Delete"},
      |        exec: function(editor) {
      |            var eodRow = editor.getSession().getDocument().getLength()-1;
      |            var eodCol = editor.session.getLine(eodRow).length;
      |            var curPos = editor.getCursorPosition();
      |            if(curPos.row == eodRow && curPos.column == eodCol) {
      |              ne=nextEditor(doc,id);
      |              if(typeof(ne)!='undefined') {
      |                console.log("merge with after!");
      |                var text = editor.getSession().getDocument().getValue();
      |                ne.focus();
      |                ne.navigateTo(0, 0);
      |                ne.insert(text+'\n', 1);
      |                document.getElementById('remove' + id).click();
      |              }
      |            } else {
      |              editor.remove("right");
      |            }
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "modeWolfe",
      |        bindKey: {win: "Ctrl-W", mac: "Ctrl-W"},
      |        exec: function(editor) {
      |            console.log("changing mode to wolfe");
      |            changeMode(id,'wolfe');
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "modeScala",
      |        bindKey: {win: "Ctrl-S", mac: "Ctrl-S"},
      |        exec: function(editor) {
      |            console.log("changing mode to scala");
      |            changeMode(id,'scala');
      |        }
      |    })
      |    editor.commands.addCommand({
      |        name: "modeMarkdown",
      |        bindKey: {win: "Ctrl-M", mac: "Ctrl-M"},
      |        exec: function(editor) {
      |            console.log("changing mode to markdown");
      |            changeMode(id,'markdown');
      |        }
      |    })
      |    //heightUpdateFunction(editor, '#editor'+id);
      |    return editor;
      |}
    """.stripMargin format(if (inEditorView) "tomorrow" else aceTheme, editorMode, initialValue)

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
      |  newDiv.classList.remove("ace-%s");
      |  oldDiv.parentNode.replaceChild(newDiv, oldDiv)
      |}
    """.stripMargin.format(aceTheme)

  // javascript that extracts the code from the editor and creates a default input
  def editorToInput: String =
    """
      |function (doc, id) {
      |  input = {}
      |  input.code = doc.cells[id].editor.getSession().getValue();
      |  return input;
      |};
    """.stripMargin
}

/**
 * Compiler where the editor is a basic HTML TextInput
 */
trait TextInputEditor {
  this: Compiler =>
  // def outputFormat: OutputFormats.Value = OutputFormats.html

  def fieldLabel: String

  def initialValue: String = ""

  def editorJavascript(inEditorView:Boolean): String =
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
    """.stripMargin format(initialValue, fieldLabel, initialValue)

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
      |  return input;
      |};
    """.stripMargin
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
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result(input.code)
  }
}

class HeadingCompiler(val level: Int) extends Compiler with TextInputEditor {
  def name: String = "heading" + level

  def fieldLabel: String = "Heading"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<span class=\"glyphicon glyphicon-header\">%d</span>" format (level)

  def compile(input: Input): Result = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result("<h%d>%s</h%d>" format(level, input.code, level))
  }
}

class SectionCompiler extends Compiler with TextInputEditor {
  def name: String = "section"

  def fieldLabel: String = "Section Name"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "&lt;#&gt;"

  def compile(input: Input): Result = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result("<h5 id=\"%s\" class=\"section\"><a href=\"#%s\" class=\"muted\"><small>#%s</small></a></h5>\n<hr/>" format(input.code, input.code, input.code))
  }
}

class ImageURLCompiler extends Compiler with TextInputEditor {
  def name: String = "imageurl"

  def fieldLabel: String = "url"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<i class=\"fa fa-picture-o\"></i>"

  def compile(input: Input): Result = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result("<img src=\"%s\" class=\"img-thumbnail displayed\" />" format (input.code))
  }
}

/**
 * Basic markdown compiler using Actuarius
 */
class ActuariusCompiler extends Compiler with ACEEditor {

  import eu.henkelmann.actuarius.ActuariusTransformer

  def name = "markdown"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<span class=\"octicon octicon-markdown\" style=\"font-size: 16px\"></span>" //"&Mu;d"

  def compile(input: Input) = {
    val transformer = new ActuariusTransformer()

    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result(transformer(input.code))
  }
}

/**
 * Basic markdown compiler using Pegdown
 */
class PegdownCompiler extends Compiler with ACEEditor {

  import org.pegdown.PegDownProcessor

  def name = "markdown"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<span class=\"octicon octicon-markdown\" style=\"font-size: 16px\"></span>" //"&Mu;d"

  def compile(input: Input) = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    val transformer = new PegDownProcessor(org.pegdown.Extensions.FENCED_CODE_BLOCKS)
    Result(transformer.markdownToHtml(input.code))
  }
}

class LatexCompiler(c: MoroConfig) extends Compiler with ACEEditor {
  def name = "latex"

  override val config = c.config(this)

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<i class=\"fa fa-superscript\"></i>"

  def compile(input: Input) = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result("$$" + input.code + "$$")
  }

  override def prefixHTML = {
    import scala.collection.JavaConversions._
    config.map(cfg => {
      cfg.getStringList("imports").map(macros => {
        val sb = new StringBuilder()
        sb.append("\n\\(\n")
        macros.foreach(str => {
          sb.append("  %s\n".format(str))
        })
        sb.append("\\)".stripMargin)
        sb.toString()
      }).getOrElse("")
    }).getOrElse("")
  }

}

trait Caching extends Compiler {
  def config: Option[Configuration]

  def maxCacheSize = config.map(c => c.getInt("maxCacheSize").getOrElse(100)).getOrElse(100)

  type CacheEntry = Input

  lazy val _cache = new Cache[Input, Result](maxCacheSize)

  override def process(input: Input): Result = {
    import CompilerConfigKeys._
    val useCache = input.config.getOrElse(CacheResults, "true").toBoolean
    if (!useCache) return super.process(input)
    _cache.getOrElseUpdate(input, super.process(input))
  }
}

class GoogleDocsViewer extends Compiler with TextInputEditor {
  // name of the compiler that should be unique in a collection of compilers
  def name: String = "google_viewer"

  def fieldLabel: String = "url"

  // icon that is used in the toolbar
  override def toolbarIcon: String = "<i class=\"fa fa-eye\"></i>"

  def compile(input: Input) = {
    //assert(input.outputFormat equalsIgnoreCase outputFormat)
    Result(
      "<iframe src=\"http://docs.google.com/viewer?url=" + input.code + "&embedded=true\" style=\"width:800px; height:600px;\" frameborder=\"0\"></iframe>")
  }
}

class RawCompiler extends Compiler with TextInputEditor {
  override def name: String = "raw"

  override def compile(input: Input): Result = Result(input.code)

  override def fieldLabel: String = "Injected Code"
}

class RawOutsideCompiler extends RawCompiler {
  override def name: String = "rawOutside"
}

class PdflatexCompiler extends Compiler with TextInputEditor {
  override def name: String = "pdflatex"

  override def compile(input: Input): Result = {
    //Document.tempDir

    val userDir = System.getProperty("user.dir")
    val tmp = new File(userDir + "/public/tmp")
    tmp.delete()
    tmp.mkdir()
    val dir = java.io.File.createTempFile("/pdf", System.nanoTime().toString, tmp)


    //val dir = java.io.File.createTempFile("/pdf", System.nanoTime().toString)
    dir.delete()
    dir.mkdir()

    val pathToFile = dir.getCanonicalPath + "/tmp.tex"
    val latexWriter = new FileWriter(pathToFile)
    latexWriter.write(input.code)
    latexWriter.close()

    import scala.sys.process._
    val pathDir = new File(dir.getCanonicalPath)

    //blocks until finished
    println(Process("pdflatex -interaction nonstopmode -shell-escape tmp.tex", pathDir).!!)

    //now tmp.pdf available
    val moroPathToPDF = "/assets" + dir.getCanonicalPath.substring(userDir.length + 7) + "/tmp.pdf"
    //val moroPathToPDF = "file:/" + dir.getCanonicalPath + "/tmp.pdf"
    println("path: " + moroPathToPDF)

    val scale = input.extraFields.getOrElse("scale", "1.0").toDouble

    val png = input.extraFields.getOrElse("png", "false").toBoolean

    if (!png) {
      val pdfScale = scale * 3.0
      val canvasId = System.nanoTime().toString
      Result(
        s"""
           |<canvas id="$canvasId"/>
           |<script>
           |  displayPDF("$moroPathToPDF", "$canvasId", "$pdfScale");
           |</script>
      """.stripMargin)
    } else {
      val dpi = scale * 200
      println(Process(s"convert -density $dpi tmp.pdf -quality 90 tmp.png", pathDir).!!)
      Result(
        s"""
           |<img src="${moroPathToPDF.dropRight(4) + ".png"}">
        """.stripMargin, OutputFormats.html)
    }
  }

  override def fieldLabel: String = "Latex Code"
}