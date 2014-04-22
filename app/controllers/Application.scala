package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Form
import controllers.doc._
import scala.collection.mutable.ArrayBuffer
import play.api.libs.json.Json
import controllers.util.{MoroConfig, MiscUtils, JacksonWrapper}
import java.io.File

object Application extends Controller {

  val config = new MoroConfig(Play.current.configuration.getConfig("moro").get)

  def index = Action {
    Redirect(routes.Application.dir(""))
    //Ok(views.html.index())
  }

  def compileJson(name: String) = Action {
    request =>
      AllCompilers.get(name).fold(BadRequest("Expecting Json data"))(
        compiler =>
          request.body.asJson.map {
            json =>
              val input = JacksonWrapper.deserialize[Input](json.toString())
              Ok(JacksonWrapper.serialize(compiler.compile(input)))
          }.getOrElse {
            BadRequest("Expecting Json data")
          })
  }

  object Notebook {
    /*
    def doc = {
      val d = new Document("Wolfe tutorial", ArrayBuffer(
        Cell(0, "heading1", Input("Heading")),
        Cell(1, "heading2", Input("Heading")),
        Cell(2, "heading3", Input("Heading")),
        Cell(3, "heading4", Input("Heading")),
        Cell(4, "heading5", Input("Heading")),
        Cell(4, "markdown", Input("This notebook can support **bold**, _italics_, [Images](http://sameersingh.org/), and `Latex` too!")),
        Cell(5, "latex", Input("\\alpha+\\beta=\\sum_{i=0}^{n}\\gamma\\cfrac{1}{\\mathcal{Z}}")),
        Cell(6, "markdown", Input("And the equations can be inlined if definining \\\\(\\theta\\\\) and \\\\(\\pi\\\\) in text.")),
        Cell(7, "imageurl", Input("http://www0.cs.ucl.ac.uk/people/photos/S.Riedel.jpg")),
        Cell(8, "scala", Input("def f(x:Int) = x * x\nf(10)")),
        Cell(9, "heading3", Input("Another Example")),
        Cell(10, "scala", Input("def f2(x:Int) = x + x\nf2(10)"))
      ))
      //Document.save(d, "public/docs/test.json")
      Document.load(Application.getClass.getResourceAsStream("/public/docs/test.json"))
    }
     */
  }

  def editor(file: String) = Action {
    if(config.editor) {
      import Document._
      println("/public/docs/" + file + ".json")
      Ok(views.html.editor(toDData(load("public/docs/" + file + ".json")), file, AllCompilers))
    } else Forbidden("Editing not allowed.")
  }

  def staticDoc(file: String) = Action {
    import Document._
    println("/public/docs/" + file + ".json")
    Ok(views.html.static(load("public/docs/" + file + ".json"), AllCompilers))
  }

  def save(file: String) = Action {
    request =>
      request.body.asJson.map {
        json => val d = Document.loadJson(json.toString())
          println(d + " --> " + file)
          println(routes.Assets.at("public/docs/" + file + ".json"))
          Document.save(d, "public/docs/" + file + ".json")
          Ok("Save successful: " + d)
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

  def dir(path: String) = Action {
    println("path: " + path)
    val dir = new Directory(path)
    println(dir)
    Ok(views.html.dir(dir, config))
  }

  def dirAddFile(path: String) = Action {
    request =>
      request.body.asJson.map {
        json => {
          try {
            val title = (json \ "title").as[String]
            val name = (json \ "name").as[String]
            val fname = if (path == "") "public/docs/" + name + ".json" else "public/docs/" + path + "/" + name + ".json"
            val d = new Document(title)
            Document.save(d, fname)
            println("fname: %s, title: %s, name: %s, doc: %s" format(fname, title, name, d))
            Ok("success")
          } catch {
            case e: Exception => BadRequest("Exception: " + e.getStackTrace.mkString("\n\t"))
          }
        }
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

  def dirRemoveFile(path: String) = Action {
    request =>
      request.body.asJson.map {
        json => {
          try {
            val name = (json \ "name").as[String]
            val fname = if (path == "") "public/docs/" + name + ".json" else "public/docs/" + path + "/" + name + ".json"
            val f = new File(fname)
            println("fname: %s, name: %s" format(fname, name))
            f.delete()
            Ok("success")
          } catch {
            case e: Exception => BadRequest("Exception: " + e.getStackTrace.mkString("\n\t"))
          }
        }
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

  def dirAddFolder(path: String) = Action {
    request =>
      request.body.asJson.map {
        json => {
          try {
            val name = (json \ "name").as[String]
            val fname = if (path == "") "public/docs/" + name else "public/docs/" + path + "/" + name
            val f = new File(fname)
            println("fname: %s, name: %s" format(fname, name))
            f.mkdir()
            Ok("success")
          } catch {
            case e: Exception => BadRequest("Exception: " + e.getStackTrace.mkString("\n\t"))
          }
        }
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

  def dirRemoveFolder(path: String) = Action {
    request =>
      request.body.asJson.map {
        json => {
          try {
            val name = (json \ "name").as[String]
            val fname = if (path == "") "public/docs/" + name else "public/docs/" + path + "/" + name
            val f = new File(fname)
            println("fname: %s, name: %s" format(fname, name))
            MiscUtils.delete(f)
            Ok("success")
          } catch {
            case e: Exception => BadRequest("Exception: " + e.getStackTrace.mkString("\n\t"))
          }
        }
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

}