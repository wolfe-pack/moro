package controllers

import play.api._
import play.api.mvc._
import controllers.doc._
import controllers.util.{MoroConfig, MiscUtils, JsonWrapper}
import java.io.File

object Application extends Controller {

  val UserAwareAction = Action

  val config = new MoroConfig(Play.current.configuration.getConfig("moro").get)
  val allCompilers = new AllCompilers(config)

  def index = UserAwareAction {
    implicit request =>
      Redirect(routes.Application.dir(""))
    //Ok(views.html.index())
  }

  def compileJson(name: String) = Action {
    request =>
      allCompilers.get(name).fold(BadRequest("Illegal compiler: " + name))(
        compiler =>
          request.body.asJson.map {
            json =>
              val input = JsonWrapper.deserialize[Input](json.toString())
              Ok(JsonWrapper.serialize(compiler.process(input)))
          }.getOrElse {
            BadRequest("Expecting Json data")
          })
  }

  def editor(file: String) =
    UserAwareAction {
      implicit request =>
        if (config.editorEnabled) {
          import Document._
          println("/editor: " + config.docRoot + file + ".json")
          Ok(views.html.editor(toDData(loadDoc(config.docRoot + file + ".json")), file, allCompilers, config,
            None)) //request.user.map(_.asInstanceOf[MoroUser])
        } else BadRequest("Editing not allowed")
    }

  /*else SecuredAction {
       implicit request =>
         import Document._
         println(request.user)
         println(config.docRoot + file + ".json")
         Ok(views.html.editor(toDData(load(config.docRoot + file + ".json")), file, allCompilers, config,
           Some(request.user.asInstanceOf[MoroUser])))
     }*/

  // adapted from http://thomasheuring.wordpress.com/2013/01/29/scala-playframework-2-04-get-pages-dynamically/
  object Dynamic {

    def render(keyword: String, file: String, user: Option[MoroUser]): Option[play.twirl.api.Html] = {
      renderDynamic("views.html." + keyword, file: String, user)
    }

    def renderDynamic(viewClazz: String, file: String, user: Option[MoroUser]): Option[play.twirl.api.Html] = {
      try {
        val clazz: Class[_] = Play.current.classloader.loadClass(viewClazz)
        val render = clazz.getDeclaredMethod("apply", classOf[Document], classOf[Compilers], classOf[String], classOf[Option[MoroUser]])
        val view = render.invoke(clazz, Document.loadDocWithCache(config.docRoot + file + ".json"), allCompilers, config.docRoot, user).asInstanceOf[play.twirl.api.Html]
        return Some(view)
      } catch {
        case ex: ClassNotFoundException => Logger.error("Html.renderDynamic() : could not find view " + viewClazz, ex)
      }

      return None
    }
  }

  def template(name: String, file: String) = UserAwareAction {
    implicit request =>
      println("/template: %s: %s" format(name, file))
      Dynamic.render(name, file, None) match {
        //request.user.map(_.asInstanceOf[MoroUser])
        case Some(i) => Ok(i)
        case None => NotFound("template: " + name)
      }
  }

  def staticDoc(file: String) = UserAwareAction {
    implicit request =>
      import Document._
      println("/static: " + config.docRoot + file + ".json")
      Ok(views.html.static(loadDocWithCache(config.docRoot + file + ".json"), allCompilers, config.docRoot,
        None)) //request.user.map(_.asInstanceOf[MoroUser])
  }

  def presentDoc(file: String) = UserAwareAction {
    implicit request =>
      import Document._
      println("/present: " + config.docRoot + file + ".json")
      Ok(views.html.present(loadDocWithCache(config.docRoot + file + ".json"), allCompilers, config.docRoot,
        None)) //request.user.map(_.asInstanceOf[MoroUser])
  }

  def wolfeStaticDoc(file: String) = UserAwareAction {
    implicit request =>
      import Document._
      println("/wolfe: %s (%s%s.json)" format(file, config.docRoot, file))
      Ok(views.html.wolfe(loadDocWithCache(config.docRoot + file + ".json"), allCompilers, config.docRoot,
        None)) // request.user.map(_.asInstanceOf[MoroUser])
  }

  def save(file: String) = UserAwareAction(parse.json(maxLength = 1024 * 500)) {
    implicit request =>
      val json = request.body
      val d = Document.loadJson(json.toString())
      println("/save: " + d + " --> " + file)
      Document.save(d, config.docRoot + file + ".json")
      Ok("Save successful: " + d)
  }

  def dir(path: String) = UserAwareAction {
    implicit request =>
      println("/dir: " + path)
      val dir = new Directory(path, config.docRoot)
      println(dir)
      Ok(views.html.dir(dir, config, None)) //request.user.map(_.asInstanceOf[MoroUser])
  }

  def dirAddFile(path: String) = UserAwareAction {
    implicit request =>
      request.body.asJson.map {
        json => {
          try {
            val title = (json \ "title").as[String]
            val name = (json \ "name").as[String]
            val fname = if (path == "") config.docRoot + name + ".json" else config.docRoot + path + "/" + name + ".json"
            val d = new Document(title)
            Document.save(d, fname)
            println("/dirAdd: " + "fname: %s, title: %s, name: %s, doc: %s" format(fname, title, name, d))
            Ok("success")
          } catch {
            case e: Exception => BadRequest("Exception: " + e.getStackTrace.mkString("\n\t"))
          }
        }
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

  def dirRemoveFile(path: String) = UserAwareAction {
    implicit request =>
      request.body.asJson.map {
        json => {
          try {
            val name = (json \ "name").as[String]
            val fname = if (path == "") config.docRoot + name + ".json" else config.docRoot + path + "/" + name + ".json"
            val f = new File(fname)
            println("/dirRemove: " + "fname: %s, name: %s" format(fname, name))
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

  def dirAddFolder(path: String) = UserAwareAction {
    implicit request =>
      request.body.asJson.map {
        json => {
          try {
            val name = (json \ "name").as[String]
            val fname = if (path == "") config.docRoot + name else config.docRoot + path + "/" + name
            val f = new File(fname)
            println("/dirAddFolder: " + "fname: %s, name: %s" format(fname, name))
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

  def dirRemoveFolder(path: String) = UserAwareAction {
    implicit request =>
      request.body.asJson.map {
        json => {
          try {
            val name = (json \ "name").as[String]
            val fname = if (path == "") config.docRoot + name else config.docRoot + path + "/" + name
            val f = new File(fname)
            println("/dirRemoveFolder: " + "fname: %s, name: %s" format(fname, name))
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

  def autocompleteScala(sessionId: String) = Action {
    request =>
      request.body.asJson.map {
        json => Ok(JsonWrapper.serialize(
          allCompilers.get("scala").fold(Seq.empty[String])(
            compiler => {
              val line = (json \ "line").as[String]
              val prefix = (json \ "prefix").as[String]
              val intp = compiler.asInstanceOf[ScalaServer].interpreter
              println("/autoComp: " + "Trying to autocomplete, line: " + line + ", prefix: " + prefix)
              intp.autocompleteLine(sessionId, line) ++ intp.autocomplete(sessionId, prefix)
            })))
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }
}
