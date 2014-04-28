name := "moro"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers ++= Seq(
  "IESL Release" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  Resolver.sonatypeRepo("snapshots") //,
  //"Wolfe Release" at "http://homeniscient.cs.ucl.ac.uk:8081/nexus/content/repositories/releases",
  //"Wolfe Snapshots" at "http://homeniscient.cs.ucl.ac.uk:8081/nexus/content/repositories/snapshots"
)

//libraryDependencies ++= Seq(
//  jdbc,
//  anorm,
//  cache
//)

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

libraryDependencies ++= Seq(
  "net.sf.trove4j" % "trove4j" % "3.0.3",
  "org.scalautils" % "scalautils_2.10" % "2.0",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided",
//  "cc.factorie" % "factorie" % "1.0.0-M7",
//  "default" %% "scalapplcodefest" % "0.1.0",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.2.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2",
  "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6",
  "org.scala-lang" % "scala-compiler" % "2.10.3",
  "ml.wolfe" %% "wolfe-core" % "0.1.0-SNAPSHOT",
  "org.sameersingh.htmlgen" % "htmlgen" % "0.1-SNAPSHOT",
  "org.scala-lang" % "scala-library" % "2.10.3"
)

//val scalaCompiler = "org.scala-lang" % "scala-compiler" % "2.10.1"

play.Project.playScalaSettings
