package controllers

import controllers.util.MoroConfig
import collection.JavaConverters._

/**
 * @author Sebastian Riedel
 */
/**
 * Scala server using the Twitter Eval implementation
 */
class WolfeEvalServer(c: MoroConfig) extends ScalaServer(c) {

  override def name = "wolfe"

  override def aceTheme: String = "wolfe"

  override def editorMode = "scala"
}
