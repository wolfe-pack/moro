package controllers

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{IMain, ILoop}
import java.io.File

/**
 * @author sameer
 */

trait Compiler {
  def compile(code: String): String

  def stop {}

  def start {}

  def reset {}
}

class ActuriusCompiler extends Compiler {
  import eu.henkelmann.actuarius.ActuariusTransformer
  val transformer = new ActuariusTransformer()
  def compile(code: String) = transformer(code)
}

class LatexCompiler extends Compiler {
  def compile(latex: String) = "$$" + latex + "$$"
}

class TwitterEvalServer extends Compiler {

  def compile(code: String) = {
    val eval = new Evaluator(None, Classpath.paths) // List("/Users/sameer/src/research/interactiveppl/lib/scalapplcodefest_2.10-0.1.0.jar"))
    println("compiling code : " + code)
    val result = try {
      eval.apply[Any](code, false)
    } catch {
      case e: CompilerException => e.m.mkString("\n\t")
    } finally {
      "Compile Error!!"
    }
    println("result: " + result)
    result.toString
  }
}

object Classpath {
  val rawPaths = """- /Users/sameer/src/research/interactiveppl/target/scala-2.10/classes
                   |	- /Users/sameer/src/research/interactiveppl/lib/scala-compiler.jar
                   |	- /Users/sameer/src/research/interactiveppl/lib/scala-library.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/scala_2.10/sbt_0.13/com.github.mpeltonen/sbt-idea/jars/sbt-idea-1.5.2.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/commons-io/commons-io/2.0.1/jars/commons-io.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play-jdbc_2.10/2.2.1/jars/play-jdbc_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play_2.10/2.2.1/jars/play_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/sbt-link/2.2.1/jars/sbt-link.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.javassist/javassist/3.18.0-GA/bundles/javassist.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play-exceptions/2.2.1/jars/play-exceptions.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/templates_2.10/2.2.1/jars/templates_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.github.scala-incubator.io/scala-io-file_2.10/0.4.2/jars/scala-io-file_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.github.scala-incubator.io/scala-io-core_2.10/0.4.2/jars/scala-io-core_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.jsuereth/scala-arm_2.10/1.3/jars/scala-arm_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play-iteratees_2.10/2.2.1/jars/play-iteratees_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.scala-stm/scala-stm_2.10/0.7/jars/scala-stm_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe/config/1.0.2/bundles/config.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play-json_2.10/2.2.1/jars/play-json_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play-functional_2.10/2.2.1/jars/play-functional_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play-datacommons_2.10/2.2.1/jars/play-datacommons_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/joda-time/joda-time/2.2/jars/joda-time.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.joda/joda-convert/1.3.1/jars/joda-convert.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.fasterxml.jackson.core/jackson-annotations/2.2.2/jars/jackson-annotations.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.fasterxml.jackson.core/jackson-core/2.2.2/jars/jackson-core.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.fasterxml.jackson.core/jackson-databind/2.2.2/jars/jackson-databind.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/sbt/boot/scala-2.10.2/lib/scala-reflect.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/io.netty/netty/3.7.0.Final/bundles/netty.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.netty/netty-http-pipelining/1.1.2/jars/netty-http-pipelining.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.slf4j/slf4j-api/1.7.5/jars/slf4j-api.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.slf4j/jul-to-slf4j/1.7.5/jars/jul-to-slf4j.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.slf4j/jcl-over-slf4j/1.7.5/jars/jcl-over-slf4j.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/ch.qos.logback/logback-core/1.0.13/jars/logback-core.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/ch.qos.logback/logback-classic/1.0.13/jars/logback-classic.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.akka/akka-actor_2.10/2.2.0/jars/akka-actor_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.akka/akka-slf4j_2.10/2.2.0/bundles/akka-slf4j_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.apache.commons/commons-lang3/3.1/jars/commons-lang3.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.ning/async-http-client/1.7.18/jars/async-http-client.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/oauth.signpost/signpost-core/1.2.1.2/jars/signpost-core.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/commons-codec/commons-codec/1.3/jars/commons-codec.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/oauth.signpost/signpost-commonshttp4/1.2.1.2/jars/signpost-commonshttp4.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.apache.httpcomponents/httpcore/4.0.1/jars/httpcore.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.apache.httpcomponents/httpclient/4.0.1/jars/httpclient.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/commons-logging/commons-logging/1.1.1/jars/commons-logging.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/xerces/xercesImpl/2.11.0/jars/xercesImpl.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/xml-apis/xml-apis/1.4.01/jars/xml-apis.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/javax.transaction/jta/1.1/jars/jta.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.jolbox/bonecp/0.8.0.RELEASE/bundles/bonecp.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.google.guava/guava/14.0.1/bundles/guava.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.h2database/h2/1.3.172/jars/h2.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/tyrex/tyrex/1.0.1/jars/tyrex.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/anorm_2.10/2.2.1/jars/anorm_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/com.typesafe.play/play-cache_2.10/2.2.1/jars/play-cache_2.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/net.sf.ehcache/ehcache-core/2.6.6/jars/ehcache-core.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/net.sf.trove4j/trove4j/jars/trove4j-3.0.3.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.scalautils/scalautils_2.10/jars/scalautils_2.10-2.0.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/cc.factorie/factorie/jars/factorie-1.0.0-M7.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.scala-lang/scala-compiler/jars/scala-compiler-2.10.1.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/junit/junit/jars/junit-4.10.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.hamcrest/hamcrest-core/jars/hamcrest-core-1.1.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.mongodb/mongo-java-driver/jars/mongo-java-driver-2.11.1.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/net.sourceforge.jregex/jregex/jars/jregex-1.2_01.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.jblas/jblas/jars/jblas-1.2.3.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/default/scalapplcodefest_2.10/jars/scalapplcodefest_2.10-0.1.0.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.scala-lang/scala-library/jars/scala-library-2.10.3.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/com.github.axel22/scalameter_2.10/jars/scalameter_2.10-0.4.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.scalatest/scalatest_2.10/jars/scalatest_2.10-1.9.1.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.scala-lang/scala-actors/jars/scala-actors-2.10.0.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/jfree/jfreechart/jars/jfreechart-1.0.12.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/jfree/jcommon/jars/jcommon-1.0.15.jar
                   |	- /Users/sameer/src/third/play-2.2.1/framework/../repository/cache/org.apache.commons/commons-math3/jars/commons-math3-3.0.jar
                   |	- /Users/sameer/src/third/play-2.2.1/repository/local/org.scala-tools.testing/test-interface/0.5/jars/test-interface.jar""".replaceAll("\\|", "").replaceAll("\\s\\-\\s", "").replaceAll(" +", "").replaceAll("\\n", ",").split(",").map(_.trim).toList
  def paths = rawPaths
}