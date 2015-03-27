package controllers

import _root_.java.io.{PrintWriter, FileOutputStream}

import play.api.{Application => PlayApplication}
import play.api.libs.Codecs
import play.api.libs.json.{Json, JsValue, Writes}
//import securesocial.core._
//import securesocial.core.providers.Token

import scala.collection.mutable


/**
 * Created by sameer on 10/1/14.
 */
class MoroUserService(application: PlayApplication) { //extends UserServicePlugin(application) {

  type Token = String
  type IdentityId = Any
  type Identity = Int

  val tokens = new mutable.HashMap[String, Token]()

  /**
   * Finds a user that maches the specified id
   *
   * @param id the user id
   * @return an optional user
   */
  def find(id: IdentityId):Option[Identity] = None //UserIO.find(id)

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
    //UserIO.find(email, providerId)
    None
  }

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   * @param identity
   */
  def save(identity: Identity) = {
    val user = "" //UserIO(identity)
    //println("saving: " + user.email.get + " (" + user.identityId + ")")
    //if(UserIO.find(user.identityId).isEmpty) user.save
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
    //tokens(token.uuid) = token
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
    //val ids = tokens.filter(_._2.isExpired).map(_._1)
    //ids.foreach(tokens.remove(_))
  }
}
