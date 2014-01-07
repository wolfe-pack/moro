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
      | <div id="editor%d" class="cell light-border">%s</div>
      | <div id="renderDisplay%d">%s</div>
      | <script>
      |                var id = %d;
      |                var mode = "scala";
      |
      |                var editor = ace.edit("editor"+id);
      |                editor.setTheme("ace/theme/solarized_light");
      |                editor.getSession().setMode("ace/mode/scala");
      |                editor.focus();
      |                editor.navigateFileEnd();
      |
      |                heightUpdateFunction(editor, '#editor'+id);
      | </script>
    """.stripMargin format(id, code, id, output, id)
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
    val heading1 = "heading1"
    val heading2 = "heading2"
    val heading3 = "heading3"
    val heading4 = "heading4"
    val heading5 = "heading5"
  }

  case class CellData(id: Int, format: String, content: String, extra: Map[String, String] = Map.empty) {
    def escapedContent = content.replaceAll("\\\\", "\\\\\\\\")
  }

  def toCellData(c: Cell): CellData = c match {
    case r: RawText => CellData(r.id, Formats.raw, r.text)
    case h: HTML => CellData(h.id, Formats.html, h.text)
    case m: Markdown => CellData(m.id, Formats.markdown, m.text)
    case l: Latex => CellData(l.id, Formats.latex, l.latex, Map("surroundWithAlign" -> l.surroundWithAlign.toString))
    case s: Scala => CellData(s.id, Formats.scala, s.code, Map("output" -> s.output))
    case i: ImageURL => CellData(i.id, Formats.imageurl, i.text)
    case h1: Heading => CellData(h1.id, Formats.heading1, h1.text, Map("level" -> h1.level.toString))
  }

  def toCell(c: CellData): Cell = c.format match {
    case Formats.raw => RawText(c.id, c.content)
    case Formats.html => HTML(c.id, c.content)
    case Formats.markdown => Markdown(c.id, c.content)
    case Formats.latex => Latex(c.id, c.content, c.extra.getOrElse("surroundWithAlign", "true").toBoolean)
    case Formats.scala => Scala(c.id, c.content, c.extra.getOrElse("output", ""))
    case Formats.imageurl => ImageURL(c.id, c.content)
    case Formats.heading1 => Heading(c.id, c.content, 1)
    case Formats.heading2 => Heading(c.id, c.content, 2)
    case Formats.heading3 => Heading(c.id, c.content, 3)
    case Formats.heading4 => Heading(c.id, c.content, 4)
    case Formats.heading5 => Heading(c.id, c.content, 5)
  }
}