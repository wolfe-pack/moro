package controllers.util

import play.api.libs.json._

/**
 * @author sameer
 */
class JsonWrapper {

  def serialize[T](value: T)(implicit w: Writes[T]): String = {
    Json.stringify(Json.toJson(value))
  }

  def serializePretty[T](value: T)(implicit w: Writes[T]): String = {
    Json.prettyPrint(Json.toJson(value))
  }

  def deserialize[T](value: String)(implicit r: Reads[T]): T = Json.fromJson[T](Json.parse(value)) match {
    case js: JsSuccess[T] => js.get
    case je: JsError => throw new Error(je.toString)
  }
}

object JsonWrapper extends JsonWrapper