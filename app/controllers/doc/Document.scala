package controllers.doc

import scala.collection.mutable.ArrayBuffer
import controllers.util.JacksonWrapper
import java.io.{FileInputStream, InputStream, PrintWriter}
import scala.io.Source
import controllers.doc.Cell.CellData

/**
 * A Document that represents a single notebook.
 * Can be mutated in terms of adding/removing cells
 * Can be saved and loaded from Json.
 * @author sameer
 */
class Document(val name: String, val cells: ArrayBuffer[Cell] = new ArrayBuffer()) {

  def save(filepath: String): Unit = Document.save(this, filepath)

  override def toString = {
    "Doc(%s)\n%s" format(name, cells.mkString("\t", "\n\t", "\n"))
  }
}

object Document {

  case class DocumentData(name: String,
                          cells: Seq[CellData])

  def save(doc: Document, filepath: String) = {
    val dd = DocumentData(doc.name, doc.cells.map(c => Cell.toCellData(c)))
    val writer = new PrintWriter(filepath)
    writer.println(JacksonWrapper.serializePretty(dd))
    writer.flush()
    writer.close()
  }

  def load(is: InputStream): Document = {
    val reader = Source.fromInputStream(is)
    val dd = JacksonWrapper.deserialize[DocumentData](reader.getLines().mkString("\n"))
    reader.close()
    val doc = new Document(dd.name)
    doc.cells ++= dd.cells.map(c => Cell.toCell(c))
    doc
  }

  def load(filepath: String): Document = {
    load(new FileInputStream(filepath))
  }
}
