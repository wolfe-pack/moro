name := "moro"

organization := "ml.wolfe"

organizationHomepage := Some(url("http://www.wolfe.ml/"))

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://wolfe-pack.github.io/moro"))

resolvers ++= Seq(
  "IESL Release" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  "Wolfe Release" at "http://homeniscient.cs.ucl.ac.uk:8081/nexus/content/repositories/releases",
  "Wolfe Snapshots" at "http://homeniscient.cs.ucl.ac.uk:8081/nexus/content/repositories/snapshots"
)

// disable using the Scala version in output paths and artifacts
crossPaths := false

pomExtra := (
  <scm>
    <url>https://github.com/wolfe-pack/moro</url>
    <connection>scm:git:git://github.com/wolfe-pack/moro.git</connection>
    <developerConnection>scm:git:git@github.com:wolfe-pack/moro.git</developerConnection>
    <tag>HEAD</tag>
  </scm>
  <developers>
    <developer>
      <name>Sameer Singh</name>
      <email>sameeersingh@gmail.com</email>
      <organization>University of Washington</organization>
      <organizationUrl>http://www.sameersingh.org</organizationUrl>
    </developer>
  </developers>
  )

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishTo := {
  val nexus = "https://dev-iesl.cs.umass.edu/nexus/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "content/repositories/releases")
}

publishArtifact in Test := false

libraryDependencies ++= Seq(
  "net.sf.trove4j" % "trove4j" % "3.0.3",
  "org.scalautils" % "scalautils_2.10" % "2.0",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "cc.factorie" % "factorie" % "1.0",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.2.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2",
  "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6",
  "org.scala-lang" % "scala-compiler" % "2.10.3",
  "org.sameersingh.htmlgen" % "htmlgen" % "0.2-SNAPSHOT",
  "org.sameersingh.scalaplot" % "scalaplot" % "0.0.3",
  "ml.wolfe" %% "wolfe-core" % "0.3.0",
  "ml.wolfe" %% "wolfe-examples" % "0.3.0",
  "org.scala-lang" % "scala-library" % "2.10.3",
  "org.pegdown" % "pegdown" % "1.4.2",
  "ws.securesocial" %% "securesocial" % "2.1.4",
  "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2"
)

play.Project.playScalaSettings
