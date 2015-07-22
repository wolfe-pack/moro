package controllers.doc

import controllers.{Result, Input}
import controllers.util.JsonWrapper
import play.api.libs.json.Json

/**
 * @author sameer
 */
case class Cell(id: Int, compiler: String, input: Input) {
  def escapedContent: String = input.code.replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t").replaceAll("'", "\\\\'").replaceAll("</script>", "<\\\\/script>")
  def inputJson: String = JsonWrapper.serialize(input).replaceAll("</script>", "<\\\\/script>")
}

object Cell {
  implicit val cellWrt = Json.writes[Cell]
  implicit val cellRds = Json.reads[Cell]
}
/*
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
}*/

