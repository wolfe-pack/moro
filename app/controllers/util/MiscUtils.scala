package controllers.util

import java.io.{FileNotFoundException, File}

/**
 * Created by sameer on 1/17/14.
 */
object MiscUtils {
  /**
   * Takes a camel cased identifier name and returns an space separated name
   * from: https://gist.github.com/sidharthkuruvila/3154845
   * Example:
   * camelToUnderscores("thisIsA1Test") == "this_is_a_1_test"
   */
  def camelToSpaces(name: String, lower: Boolean = false) = "[A-Z\\d]".r.replaceAllIn(name, {
    m =>
      " " + (if (lower) m.group(0).toLowerCase() else m.group(0))
  })

  /*
   * Takes an underscore separated identifier name and returns a camel cased one
   *
   * Example:
   *    underscoreToCamel("this_is_a_1_test") == "thisIsA1Test"
   */

  def spacesToCamel(name: String) = "_([a-z\\d])".r.replaceAllIn(name, {
    m =>
      m.group(1).toUpperCase()
  })

  def delete(f: File) {
    if (f.isDirectory()) {
      for (f <- f.listFiles())
        delete(f)
    }
    if (!f.delete())
      throw new FileNotFoundException("Failed to delete file: " + f);
  }

  def escapeTags(str:String) = {
    str.replace("<", "\\<").replace(">", "\\>");
  }
}
