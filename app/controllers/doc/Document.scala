package controllers.doc

import controllers.{ConfigEntry, Input}
import play.api.libs.json.Json

import scala.collection.mutable.ArrayBuffer
import controllers.util.{MoroConfig, JacksonWrapper}
import java.io.{File, FileInputStream, InputStream, PrintWriter}
import scala.io.Source

/**
 * A Document that represents a single notebook.
 * Can be mutated in terms of adding/removing cells
 * Can be saved and loaded from Json.
 * @author sameer
 */
case class Document(val name: String, cells: Seq[Cell] = Seq.empty, config: Map[String, String] = Map.empty) {

  def save(filepath: String): Unit = Document.save(this, filepath)

  override def toString = {
    "Doc(%s)\n%s" format(name, cells.mkString("\t", "\n\t", "\n"))
  }

  def configJson: String = if (config == null) "{}" else Json.stringify(Json.toJson(config))
}

object Document {

  type DocumentData = Document

  def toDData(doc: Document) = new DocumentData(doc.name, doc.cells, doc.config)

  def save(doc: Document, filepath: String) = {
    val dd = toDData(doc)
    val writer = new PrintWriter(filepath)
    writer.println(JacksonWrapper.serializePretty(dd))
    writer.flush()
    writer.close()
  }

  def loadJson(fromJson: String): Document = {
    val dd = JacksonWrapper.deserialize[DocumentData](fromJson)
    val doc = new Document(dd.name, dd.cells, dd.config)
    doc
  }

  def load(is: InputStream): Document = {
    val reader = Source.fromInputStream(is)
    val dd = loadJson(reader.getLines().mkString("\n"))
    reader.close()
    dd
  }

  def load(filepath: String): Document = {
    load(new FileInputStream(filepath))
  }

  def configEntries: Seq[ConfigEntry] = Seq(
    ConfigEntry("autosave", "Autosave?", "This document will automatically save periodically.", "checkbox", "true")
  )

  implicit val ceWrites = Json.writes[ConfigEntry]

  def configEntriesJson: String = Json.stringify(Json.toJson(configEntries))

  val moroConfig = new MoroConfig(play.api.Play.current.configuration.getConfig("moro").get)

  lazy val tempDir: String = {
    val tmpDir = moroConfig.docRoot + "/tmp"
    val tmpFile = new File(tmpDir)
    if(tmpFile.exists()) {
      assert(tmpFile.isDirectory, tmpFile.getCanonicalPath + " is not a directory.")
      assert(tmpFile.canWrite, tmpFile.getCanonicalPath + " is not writeable.")
    } else {
      tmpFile.mkdir()
    }
    tmpDir + "/"
  }

  def main(args: Array[String]): Unit = {
    //val d = new Document("test", Seq(Cell(0, "scala", Input("10*10", OutputFormats.html, Map("fragment" -> "true")))))
    //d.save("test.json")
  }
}
