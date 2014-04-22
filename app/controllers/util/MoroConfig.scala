package controllers.util

import play.api.Configuration

/**
 * @author sameer
 * @since 4/22/14.
 */
class MoroConfig(underlying: Configuration) {
  def editor = underlying.getBoolean("editor").getOrElse(true)
}
