package controllers.util

import play.api.Configuration
import controllers.Compiler

/**
 * @author sameer
 * @since 4/22/14.
 */
class MoroConfig(underlying: Configuration) {
  def editor = underlying.getBoolean("editor").getOrElse(true)

  def config(c: Compiler): Option[Configuration] = underlying.getConfig("compilers." + c.name)
}
