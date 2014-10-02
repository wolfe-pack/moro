package controllers

import scala.collection.mutable.ArrayBuffer
import java.io.{FileInputStream, File}
import controllers.doc.Document
import java.text.SimpleDateFormat

/**
 * @author sameer
 */
class Directory(val path: String, val docRoot: String) {
  val name = new File(docRoot + path).getName

  val canonPath = if (path == "") "" else "/" + path

  val superDirs = {
    if (path == "") Seq.empty[SuperDirectory]
    else {
      val strs = (path.split("/").toSeq)
      var currentPath = ""
      (Seq(SuperDirectory(docRoot.split("/").last, "")) ++ strs.map(s => {
        currentPath = currentPath + "/" + s
        SuperDirectory(s, currentPath)
      }).toSeq).dropRight(1)
    }
  }
  val subDirs = {
    val files = new File(docRoot + path).listFiles().filter(_.isDirectory)
    files.map(f => {
      val count = f.listFiles().filterNot(_.isDirectory).size
      SubDirectory(f.getName, canonPath + "/" + f.getName, count)
    }).toSeq.sortBy(_.name)
  }

  val files = {
    val files = new File(docRoot + path).listFiles().filterNot(_.isDirectory).filter(_.getName.endsWith(".json"))
    val sdf = new SimpleDateFormat("MMM dd, yyyy KK:mm:ss a z")
    files.map(f => {
      val d = Document.load(new FileInputStream(f))
      val name = f.getName.dropRight(5)
      Notebook(name, d.name, canonPath + "/" + name, sdf.format(f.lastModified()))
    }).toSeq.sortBy(_.name)
  }

  val otherFiles = {
    val files = new File(docRoot + path).listFiles().filterNot(_.isDirectory).filterNot(_.getName.endsWith(".json"))
    files.map(f => {
      val name = f.getName
      OtherFile(name, canonPath + "/" + name)
    }).toSeq.sortBy(_.name)
  }

  override def toString: String = "Dir(%s, %s, %s)\n\t%s\n\t%s" format(name, path, superDirs, subDirs.mkString(", "), files.mkString(", "))
}

case class OtherFile(name: String, href: String)

case class SuperDirectory(name: String, href: String)

case class SubDirectory(name: String, href: String, badgeCount: Int)

case class Notebook(name: String, title: String, href: String, lastModified: String)
