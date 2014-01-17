package controllers

import scala.collection.mutable.ArrayBuffer
import java.io.File

/**
 * @author sameer
 */
trait TreeNode {
  def parent: Directory
  def name: String
  def pathToRoot: String = parent.pathToRoot + "/" + name
  def pathToBaseDir: String = parent.pathToBaseDir + "/" + name
  def toString(level:Int): String = {
    (0 until level).map(i => "  ").mkString("") + name + "\t" + pathToRoot + " : " + pathToBaseDir + "\n"
  }
}

trait Directory extends TreeNode {
  val children = new ArrayBuffer[TreeNode]
  override def toString(level:Int): String = {
    var str = new StringBuffer(super.toString(level))
    for(c <- children) {
      str append c.toString(level + 1)
    }
    str.toString
  }
}

trait Root extends Directory {
  def parent: Directory = null
  def baseDir: String
  override def pathToRoot: String = name

  override def pathToBaseDir: String = baseDir
}

class Tree(baseDir: String) {
  self =>
  val root = new Root {
    def name: String = new File(baseDir).getName

    def baseDir: String = self.baseDir
  }
  loadDir(root)
  
  def loadDir(node: Directory) {
    val file = new File(node.pathToBaseDir)
    assert(file.isDirectory)
    val dirs = new ArrayBuffer[File]
    val files = new ArrayBuffer[File]
    for(child <- file.listFiles()) {
      if(child.isDirectory) dirs += child
      else files += child
    }
    for(d <- dirs) {
      val childNode = new Directory {
        def name: String = d.getName

        def parent: Directory = node
      }
      node.children += childNode
      loadDir(childNode)
    }
    for(f <- files) {
      val childNode = new TreeNode {
        def parent: Directory = node

        def name: String = f.getName
      }
      node.children += childNode
    }
  }

  override def toString: String = root.toString(0)
}