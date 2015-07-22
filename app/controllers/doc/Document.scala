package controllers.doc

import controllers.{ConfigEntry, Input}
import play.api.libs.json._

import scala.collection.mutable.ArrayBuffer
import controllers.util.{MoroConfig, JsonWrapper}
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

  def withCache(cache: OutputCache = OutputCache()) = {
    Document(this.name, this.cells.map(c => {
      Cell(c.id, c.compiler,
        Input(c.input.sessionId, c.input.code, c.input.extraFields, cache.cells.get(c.id)))
    }),
      this.config)
  }

  def withoutCache: (Document, OutputCache) = {
    val cache = OutputCache(this.cells.filter(_.input.outputFormat.isDefined).map(c => c.id -> c.input.outputFormat.get).toMap)
    val doc = Document(this.name, this.cells.map(c => Cell(c.id, c.compiler,
      Input(c.input.sessionId, c.input.code, c.input.extraFields, None))),
      this.config)
    doc -> cache
  }
}

case class OutputCache(cells: Map[Int, String] = Map.empty)

object OutputCache {
  implicit val cellsWrt = new Writes[Map[Int,String]] {
    override def writes(o: Map[Int, String]): JsValue = JsObject(o.map(is => is._1.toString -> JsString(is._2)).toSeq)
  }
  implicit val cellsRds = new Reads[Map[Int,String]] {
    override def reads(json: JsValue): JsResult[Map[Int, String]] = json match {
      case obj: JsObject => JsSuccess(obj.value.map(o => o._1.toInt -> o._2.asInstanceOf[JsString].value).toMap)
      case JsNull => JsSuccess(Map.empty[Int, String])
      case _ => JsError("not an object for map[int,string]")
    }
  }

  implicit val cacheWrt = Json.writes[OutputCache]
  implicit val cacheRds = Json.reads[OutputCache]
}

object Document {

  type DocumentData = Document
  implicit val docWrt = Json.writes[Document]
  implicit val docRds = new Reads[Document] {
    override def reads(json: JsValue): JsResult[Document] = {
      json match {
        case obj: JsObject => {
          JsSuccess(Document(
            obj.value.get("name").map(_ match {
            case str:JsString => str.value
            case JsNull => null
          }).orNull,
            obj.value.get("cells").map(_ match {
              case arr: JsArray => arr.value.map(v => Json.fromJson[Cell](v).get)
              case JsNull => Seq.empty[Cell]
            }).getOrElse(Seq.empty[Cell]),
            obj.value.get("config").map(_ match {
              case mapObj: JsObject => mapObj.value.map(v => v._1 -> v._2.asInstanceOf[JsString].value).toMap
              case JsNull => Map.empty[String, String]
            }).getOrElse(Map.empty)))
        }
        case _ => JsError("not an object")
      }
    }
  }

  def toDData(doc: Document) = new DocumentData(doc.name, doc.cells, doc.config)

  def save(doc: Document, filepath: String) = {
    val (ddoc, cache) = doc.withoutCache
    val dd = toDData(ddoc)
    val writer = new PrintWriter(filepath)
    writer.println(JsonWrapper.serializePretty(dd))
    writer.flush()
    writer.close()
    saveCache(cache, filepath + ".cache")
  }

  def saveCache(cache: OutputCache, filepath: String): Unit ={
    val cacheWriter = new PrintWriter(filepath)
    cacheWriter.println(JsonWrapper.serializePretty(cache))
    cacheWriter.flush()
    cacheWriter.close()
  }

  def loadJson(fromJson: String): Document = {
    val dd = JsonWrapper.deserialize[DocumentData](fromJson)
    val doc = new Document(dd.name, dd.cells, dd.config)
    doc
  }

  def load(is: InputStream): Document = {
    val reader = Source.fromInputStream(is)
    val dd = loadJson(reader.getLines().mkString("\n"))
    reader.close()
    dd
  }

  def loadDoc(filepath: String): Document = {
    load(new FileInputStream(filepath))
  }

  def loadDocWithCache(filepath: String): Document = {
    val doc = load(new FileInputStream(filepath))
    val cfile = filepath + ".cache"
    val cache = if(new File(cfile).exists()) {
      val reader = Source.fromInputStream(new FileInputStream(cfile))
      val cache = JsonWrapper.deserialize[OutputCache](reader.getLines().mkString("\n"))
      reader.close()
      println(" -- reading cache: " + doc.withCache(cache) + " -- ")
      cache
    } else OutputCache()
    doc.withCache(cache)
  }

  def configEntries: Seq[ConfigEntry] = Seq(
    ConfigEntry("autosave", "Autosave?", "This document will automatically save periodically.", "checkbox", "true")
  )

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
