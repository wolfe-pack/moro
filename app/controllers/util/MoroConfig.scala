package controllers.util

import play.api.Configuration
import controllers.Compiler

/**
 * @author sameer
 * @since 4/22/14.
 */
class MoroConfig(underlying: Configuration) {
  def docRoot = underlying.getString("docRoot").getOrElse("public/docs/")

  def config(c: Compiler): Option[Configuration] = underlying.getConfig("compilers." + c.name)

  def viewConfig(name: String): Option[Configuration] = underlying.getConfig("views." + name)

  def editorConfig = viewConfig("editor")
  def editorEnabled = editorConfig.map(c => c.getBoolean("enabled").getOrElse(true)).getOrElse(true)
  def editorStaticTemplate = editorConfig.map(c => c.getString("staticView").getOrElse("static")).getOrElse("static")
}
