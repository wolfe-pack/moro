package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Form
import controllers.doc._
import scala.collection.mutable.ArrayBuffer
import play.api.libs.json.Json
import controllers.util.JacksonWrapper

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
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
  }

  def editor(file: String) = Action {
    import Document._
    Notebook.doc
    println("/public/docs/" + file + ".json")
    Ok(views.html.editor(toDData(load(Application.getClass.getResourceAsStream("/public/docs/" + file + ".json"))), file, AllCompilers))
  }

  def staticDoc(file: String) = Action {
    import Document._
    Notebook.doc
    println("/public/docs/" + file + ".json")
    Ok(views.html.static(load(Application.getClass.getResourceAsStream("/public/docs/" + file + ".json")), AllCompilers))
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
    println(path)
    val tree = new Tree(path)
    println(tree)
    Ok(views.html.dir(tree))
  }

}