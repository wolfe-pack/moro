package controllers

import _root_.java.io.{PrintWriter, FileOutputStream}

import play.api.Application
import play.api.libs.Codecs
import play.api.libs.json.{Json, JsValue, Writes}
import securesocial.core._
import securesocial.core.providers.Token

import scala.collection.mutable


/**
 * Created by sameer on 10/1/14.
 */
class MoroUserService(application: Application) extends UserServicePlugin(application) {

  val filename = "users.json"

  case class User(
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
    def updateIdentity(i: Identity): User = {
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
      val w = new PrintWriter(new FileOutputStream(filename, true), true)
      w.println(json)
      w.close()
    }
  }
  object UserIO {
    def apply(i: Identity): User = {
      new User(
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
    implicit val userWrites = Json.writes[User]
    def toJson(u: User) = Json.toJson(u)

    implicit val idReads = Json.reads[IdentityId]
    implicit val amReads = Json.reads[AuthenticationMethod]
    implicit val oa1Reads = Json.reads[OAuth1Info]
    implicit val oa2Reads = Json.reads[OAuth2Info]
    implicit val piReads = Json.reads[PasswordInfo]
    implicit val userReads = Json.reads[User]
    def fromJson(str: String): User = Json.fromJson[User](Json.parse(str)).get
    def readUsers: Seq[User] = {
      val source = io.Source.fromFile(filename, "UTF-8")
      val result = source.getLines().map(l => fromJson(l)).toBuffer
      source.close()
      result
    }
    def find(email: String, provider: String): Option[User] = {
      readUsers.find(u => u.email.map( e => e == email && u.identityId.providerId == provider).getOrElse(false)).headOption
    }
    def find(id: IdentityId): Option[User] = {
      readUsers.find(u => u.identityId == id).headOption
    }
  }

  val tokens = new mutable.HashMap[String, Token]()

  /**
   * Finds a user that maches the specified id
   *
   * @param id the user id
   * @return an optional user
   */
  def find(id: IdentityId):Option[Identity] = UserIO.find(id)

  /**
   * Finds a user by email and provider id.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation.
   *
   * @param email - the user email
   * @param providerId - the provider id
   * @return
   */
  def findByEmailAndProvider(email: String, providerId: String):Option[Identity] = {
    println("finding: " + email + " (" + providerId + ")")
    UserIO.find(email, providerId)
  }

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   * @param identity
   */
  def save(identity: Identity) = {
    val user = UserIO(identity)
    println("saving: " + user.email.get + " (" + user.identityId + ")")
    if(UserIO.find(user.identityId).isEmpty) user.save
    user
  }

  /**
   * Saves a token.  This is needed for users that
   * are creating an account in the system instead of using one in a 3rd party system.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token The token to save
   */
  def save(token: Token) = {
    tokens(token.uuid) = token
  }


  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token the token id
   * @return
   */
  def findToken(token: String): Option[Token] = tokens.get(token)

  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token id
   */
  def deleteToken(uuid: String) {
    tokens.remove(uuid)
  }

  /**
   * Deletes all expired tokens
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   */
  def deleteExpiredTokens() {
    val ids = tokens.filter(_._2.isExpired).map(_._1)
    ids.foreach(tokens.remove(_))
  }
}
