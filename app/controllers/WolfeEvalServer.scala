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

  override def editorMode = "scala"
}

class WolfeNoEvalServer(c: MoroConfig) extends WolfeEvalServer(c) {
  override def name: String = "wolfeNoEval"

  override def compile(input: Input): Result =
    Result("<blockquote>" + "" +"</blockquote>")

}
