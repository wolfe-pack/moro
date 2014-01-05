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

  object Compilers {
    val scala = new TwitterEvalServer
    val markdown = new ActuriusCompiler
    val latex = new LatexCompiler
    scala.start
    markdown.start
    latex.start
  }

  def index = Action {
    Ok(views.html.index())
  }

  def compileScalaJson = compileJson(Compilers.scala)

  def compileMarkdownJson = compileJson(Compilers.markdown)

  def compileLatexJson = compileJson(Compilers.latex)

  def compileJson(compiler: Compiler) = Action {
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
      val d = new Document("Wolfe tutorial", ArrayBuffer(
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
      Document.save(d, "public/docs/test.json")
      Document.load(Application.getClass.getResourceAsStream("/public/docs/test.json"))
    }
  }

  def editor(file: String) = Action {
    Notebook.doc
    println("/public/docs/" + file + ".json")
    Ok(views.html.editor(Document.load(Application.getClass.getResourceAsStream("/public/docs/" + file + ".json")), file))
  }

  def staticDoc(file: String) = Action {
    Notebook.doc
    println("/public/docs/" + file + ".json")
    Ok(views.html.static(Document.load(Application.getClass.getResourceAsStream("/public/docs/" + file + ".json"))))
  }

  def save(file: String) = Action {
    request =>
      request.body.asJson.map {
        json => val d = Document.loadJson(json.toString())
          println(d + " --> " + file)
          println(routes.Assets.at("public/docs/"+ file + ".json"))
          Document.save(d, "public/docs/" + file + ".json")
          Ok("Save successful: " + d)
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

}