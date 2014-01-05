package controllers.doc

/**
 * @author sameer
 */
trait Cell {
  def id: Int

  def staticHTML: String
}

case class RawText(id: Int, text: String) extends Cell {
  def staticHTML = text
}

case class HTML(id: Int, text: String) extends Cell {
  def staticHTML = text
}

case class Markdown(id: Int, text: String) extends Cell {

  def staticHTML = Cell.markdownTransformer(text)
}

case class Latex(id: Int, latex: String, surroundWithAlign: Boolean = true) extends Cell {
  def staticHTML = "$$" + latex + "$$"
}

case class Scala(id: Int, code: String, output: String) extends Cell {
  def staticHTML =
    """
      | <div id="editor%d" class=editor></div>
      | <div id="output%d" class=outputDisplay>%s</div>
      | <script>
      | var editor = ace.edit("editor%d");
      |        editor.setTheme("ace/theme/solarized_light");
      |        editor.getSession().setMode("ace/mode/scala");
      |        // editor.focus();
      |        editor.getSession().setValue("%s");
      |        editor.navigateFileEnd();
      | var outputDisplay = ace.edit("output%d");
      |        outputDisplay.setTheme("ace/theme/github");
      |        outputDisplay.getSession().setMode("ace/mode/markdown");
      |        outputDisplay.setReadOnly(true);  // false to make it editable
      |        outputDisplay.setShowPrintMargin(true);
      |        outputDisplay.setHighlightActiveLine(false);
      |        outputDisplay.setShowInvisibles(true);
      |        outputDisplay.renderer.setShowGutter(false);
      | </script>
    """.stripMargin format(id, id, output, id, code, id)
}

case class ImageURL(id: Int, text: String) extends Cell {
  def staticHTML = "<img src=\"%s\" class=\"img-thumbnail displayed\" />" format (text)
}

case class Heading(id: Int, text: String, level: Int) extends Cell {
  def staticHTML = "<h%d>%s</h%d>" format(level, text, level)
}

object Cell {

  import eu.henkelmann.actuarius.ActuariusTransformer

  val markdownTransformer = new ActuariusTransformer()

  object Formats {
    val raw = "raw"
    val html = "html"
    val markdown = "markdown"
    val latex = "latex"
    val scala = "scala"
    val imageurl = "imageurl"
    val heading = "heading"
  }

  case class CellData(id: Int, format: String, content: String, extra: Map[String, String] = Map.empty)

  def toCellData(c: Cell): CellData = c match {
    case r: RawText => CellData(r.id, Formats.raw, r.text)
    case h: HTML => CellData(h.id, Formats.html, h.text)
    case m: Markdown => CellData(m.id, Formats.markdown, m.text)
    case l: Latex => CellData(l.id, Formats.latex, l.latex, Map("surroundWithAlign" -> l.surroundWithAlign.toString))
    case s: Scala => CellData(s.id, Formats.scala, s.code, Map("output" -> s.output))
    case i: ImageURL => CellData(i.id, Formats.imageurl, i.text)
    case h: Heading => CellData(h.id, Formats.heading, h.text, Map("level" -> h.level.toString))
  }

  def toCell(c: CellData): Cell = c.format match {
    case Formats.raw => RawText(c.id, c.content)
    case Formats.html => HTML(c.id, c.content)
    case Formats.markdown => Markdown(c.id, c.content)
    case Formats.latex => Latex(c.id, c.content, c.extra("surroundWithAlign").toBoolean)
    case Formats.scala => Scala(c.id, c.content, c.extra("output"))
    case Formats.imageurl => ImageURL(c.id, c.content)
    case Formats.heading => Heading(c.id, c.content, c.extra("level").toInt)
  }
}