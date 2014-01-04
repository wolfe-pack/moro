package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Form
import controllers.doc._
import scala.collection.mutable.ArrayBuffer
import controllers.doc.Heading
import controllers.doc.Markdown
import play.api.libs.json.Json

object Application extends Controller {

  val compiler = new TwitterEvalServer // new NSCLoopServer
  compiler.start

  val modelForm = Form(
    "code" -> Forms.text
  )

  var code: String = "10"
  var result: String = "10"

  def index = Action {
    Redirect(routes.Application.language)
  }

  def language = Action {
    Ok(views.html.index(result, modelForm.fill(code)))
  }

  def compileCode = Action {
    implicit request =>
      modelForm.bindFromRequest.fold(
        errors => BadRequest(views.html.index("ERROR!", errors)),
        snippet => {
          code = snippet
          result = compiler.compile(code)
          Redirect(routes.Application.language)
        }
      )
  }

  def reset = Action {
    compiler.reset
    code = ""
    result = ""
    Redirect(routes.Application.language)
  }

  def compileJson = Action {
    request =>
      request.body.asJson.map {
        json => val code = (json \ "code").as[String];
          Ok(Json.obj("result" -> compiler.compile(code)))
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

  object Notebook {
    def doc = {
      val d = new Document("tutorial", ArrayBuffer(
        Heading(0, "Heading", 1),
        Heading(1, "Heading", 2),
        Heading(2, "Heading", 3),
        Heading(3, "Heading", 4),
        Heading(4, "Heading", 5),
        Markdown(4, "This notebook can support **bold**, _italics_, [Images](http://sameersingh.org/), and `Latex` too!"),
        Latex(5, "\\alpha+\\beta=\\sum_{i=0}^{n}\\gamma\\cfrac{1}{\\mathcal{Z}}"),
        Markdown(6, "And the equations can be inlined if definining \\\\(\\theta\\\\) and \\\\(\\pi\\\\) in text."),
        ImageURL(7, "http://www0.cs.ucl.ac.uk/people/photos/S.Riedel.jpg"),
        Scala(8, "def f(x:Int) = x * x\\nf(10)", "100"),
        Heading(9, "Another Example", 3),
        Scala(10, "def f2(x:Int) = x + x\\nf2(10)", "20")
      ))
      Document.save(d, "public/docs/doc.json")
      Document.load(Application.getClass.getResourceAsStream("/public/docs/doc.json"))
    }
  }

  def editor = TODO

  def static = Action {
    Ok(views.html.static(Notebook.doc))
  }

  def staticDoc(file: String) = Action {
    Notebook.doc
    println("/public/docs/" + file)
    Ok(views.html.static(Document.load(Application.getClass.getResourceAsStream("/public/docs/" + file))))
  }

}