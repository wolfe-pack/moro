package controllers

import play.api._
import play.api.mvc._
import controllers.doc._
import controllers.util.{MoroConfig, MiscUtils, JacksonWrapper}
import java.io.File
import securesocial.core.{Identity, Authorization}

object Application extends Controller with securesocial.core.SecureSocial {

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
              val input = JacksonWrapper.deserialize[Input](json.toString())
              Ok(JacksonWrapper.serialize(compiler.process(input)))
          }.getOrElse {
            BadRequest("Expecting Json data")
          })
  }

  def editor(file: String) =
    if (config.editorEnabled) {
      Action {
        import Document._
        println(config.docRoot + file + ".json")
        Ok(views.html.editor(toDData(load(config.docRoot + file + ".json")), file, allCompilers, config))
      }
    } else SecuredAction {
      implicit request =>
        import Document._
        println(request.user)
        println(config.docRoot + file + ".json")
        Ok(views.html.editor(toDData(load(config.docRoot + file + ".json")), file, allCompilers, config))
    }

  // adapted from http://thomasheuring.wordpress.com/2013/01/29/scala-playframework-2-04-get-pages-dynamically/
  object Dynamic {

    def render(keyword: String, file: String): Option[play.api.templates.Html] = {
      renderDynamic("views.html." + keyword, file: String)
    }

    def renderDynamic(viewClazz: String, file: String): Option[play.api.templates.Html] = {
      try {
        val clazz: Class[_] = Play.current.classloader.loadClass(viewClazz)
        val render = clazz.getDeclaredMethod("apply", classOf[Document], classOf[Compilers], classOf[String])
        val view = render.invoke(clazz, Document.load(config.docRoot + file + ".json"), allCompilers, config.docRoot).asInstanceOf[play.api.templates.Html]
        return Some(view)
      } catch {
        case ex: ClassNotFoundException => Logger.error("Html.renderDynamic() : could not find view " + viewClazz, ex)
      }

      return None
    }
  }

  def template(name: String, file: String) = Action {
    println("%s: %s" format(name, file))
    Dynamic.render(name, file) match {
      case Some(i) => Ok(i)
      case None => NotFound("template: " + name)
    }
  }

  def staticDoc(file: String) = Action {
    import Document._
    println(config.docRoot + file + ".json")
    Ok(views.html.static(load(config.docRoot + file + ".json"), allCompilers, config.docRoot))
  }

  def presentDoc(file: String) = Action {
    import Document._
    println(config.docRoot + file + ".json")
    Ok(views.html.present(load(config.docRoot + file + ".json"), allCompilers, config.docRoot))
  }

  def wolfeStaticDoc(file: String) = Action {
    import Document._
    println("wolfe: %s (%s%s.json)" format(file, config.docRoot, file))
    Ok(views.html.wolfe(load(config.docRoot + file + ".json"), allCompilers, config.docRoot))
  }

  def save(file: String) = Action {
    request =>
      request.body.asJson.map {
        json => val d = Document.loadJson(json.toString())
          println(d + " --> " + file)
          println(routes.Assets.at(config.docRoot + file + ".json"))
          Document.save(d, config.docRoot + file + ".json")
          Ok("Save successful: " + d)
      }.getOrElse {
        BadRequest("Expecting Json data")
      }
  }

  def dir(path: String) = UserAwareAction {
    implicit request =>
      println("path: " + path)
      val dir = new Directory(path, config.docRoot)
      println(dir)
      Ok(views.html.dir(dir, config, request.user.map(_.asInstanceOf[MoroUser])))
  }

  def dirAddFile(path: String) = Action {
    request =>
      request.body.asJson.map {
        json => {
          try {
            val title = (json \ "title").as[String]
            val name = (json \ "name").as[String]
            val fname = if (path == "") config.docRoot + name + ".json" else config.docRoot + path + "/" + name + ".json"
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
            val fname = if (path == "") config.docRoot + name + ".json" else config.docRoot + path + "/" + name + ".json"
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
            val fname = if (path == "") config.docRoot + name else config.docRoot + path + "/" + name
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
            val fname = if (path == "") config.docRoot + name else config.docRoot + path + "/" + name
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