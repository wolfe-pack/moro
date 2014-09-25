package controllers

import scala.collection.mutable
import controllers.util.MoroConfig

/**
 * Maintain a list of compilers, which is used for constructing an application
 * @author sameer
 */
class Compilers extends mutable.Traversable[Compiler] {
  val _map = new mutable.LinkedHashMap[String, Compiler]

  def apply(name: String): Compiler = _map(name)

  def get(name: String): Option[Compiler] = _map.get(name)

  def +=(c: Compiler) = {
    _map(c.name) = c
  }

  def foreach[U](f: (Compiler) => U): Unit = _map.valuesIterator.foreach(f(_))
}

class AllCompilers(config: MoroConfig) extends Compilers {
  this += new TwitterEvalServer(config)
  this += new WolfeEvalServer(config)
  this += new ActuriusCompiler
  this += new SectionCompiler
  this += new EndSectionCompiler
  this += new LatexCompiler
  for (i <- 1 to 5) this += new HeadingCompiler(i)
  this += new HTMLCompiler
  this += new ImageURLCompiler
  this += new GoogleDocsViewer
  this.foreach(_.start)
}