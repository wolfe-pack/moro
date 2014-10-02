package controllers

import play.api.libs.Codecs
import play.api.libs.json.Json
import java.io.{File, FileOutputStream, PrintWriter}
import securesocial.core._
import securesocial.core.providers.Token

/**
 * @author sameer
 * @since 10/2/14.
 */
case class MoroUser(
                 id: Option[Long],
                 identityId: IdentityId,
                 firstName: String,
                 lastName: String,
                 email: Option[String],
                 avatarUrl: Option[String],
                 authMethod: AuthenticationMethod,
                 oAuth1Info: Option[OAuth1Info],
                 oAuth2Info: Option[OAuth2Info],
                 passwordInfo: Option[PasswordInfo]
                 ) extends Identity {
  def fullName: String = s"$firstName $lastName"
  def avatar: Option[String] = avatarUrl.orElse {
    email.map { e => s"http://www.gravatar.com/avatar/${Codecs.md5(e.getBytes)}.png" }
  }
  def updateIdentity(i: Identity): MoroUser = {
    this.copy(
      identityId = i.identityId,
      firstName = i.firstName,
      lastName = i.lastName,
      email = i.email,
      authMethod = i.authMethod,
      avatarUrl = i.avatarUrl,
      oAuth1Info = i.oAuth1Info,
      oAuth2Info = i.oAuth2Info,
      passwordInfo = i.passwordInfo
    )
  }
  def save: Unit = {
    val json = Json.stringify(UserIO.toJson(this))
    println("writing: " + json)
    val w = new PrintWriter(new FileOutputStream(UserIO.filename, true), true)
    w.println(json)
    w.close()
  }
}
object UserIO {
  val filename = "users.json"

  def apply(i: Identity): MoroUser = {
    new MoroUser(
      id = None,
      identityId = i.identityId,
      firstName = i.firstName,
      lastName = i.lastName,
      email = i.email,
      authMethod = i.authMethod,
      avatarUrl = i.avatarUrl,
      oAuth1Info = i.oAuth1Info,
      oAuth2Info = i.oAuth2Info,
      passwordInfo = i.passwordInfo
    )
  }
  implicit val idWrites = Json.writes[IdentityId]
  implicit val amWrites = Json.writes[AuthenticationMethod]
  implicit val oa1Writes = Json.writes[OAuth1Info]
  implicit val oa2Writes = Json.writes[OAuth2Info]
  implicit val piWrites = Json.writes[PasswordInfo]
  implicit val userWrites = Json.writes[MoroUser]
  def toJson(u: MoroUser) = Json.toJson(u)

  implicit val idReads = Json.reads[IdentityId]
  implicit val amReads = Json.reads[AuthenticationMethod]
  implicit val oa1Reads = Json.reads[OAuth1Info]
  implicit val oa2Reads = Json.reads[OAuth2Info]
  implicit val piReads = Json.reads[PasswordInfo]
  implicit val userReads = Json.reads[MoroUser]
  def fromJson(str: String): MoroUser = Json.fromJson[MoroUser](Json.parse(str)).get
  def readUsers: Seq[MoroUser] = {
    if(new File(filename).exists()) {
      val source = io.Source.fromFile(filename, "UTF-8")
      val result = source.getLines().map(l => fromJson(l)).toBuffer
      source.close()
      result
    } else Seq.empty
  }
  def find(email: String, provider: String): Option[MoroUser] = {
    readUsers.find(u => u.email.map( e => e == email && u.identityId.providerId == provider).getOrElse(false)).headOption
  }
  def find(id: IdentityId): Option[MoroUser] = {
    readUsers.find(u => u.identityId == id).headOption
  }
}

