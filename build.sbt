name := "moro"

organization := "ml.wolfe"

organizationHomepage := Some(url("http://www.wolfe.ml/"))

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.4"

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://wolfe-pack.github.io/moro"))

resolvers ++= Seq(
  Resolver.file("Local repo", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),
  "IESL Release" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public",
  Resolver.mavenLocal,
  Resolver.defaultLocal,
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  "Wolfe Release" at "http://homeniscient.cs.ucl.ac.uk:8081/nexus/content/repositories/releases",
  "Wolfe Snapshots" at "http://homeniscient.cs.ucl.ac.uk:8081/nexus/content/repositories/snapshots",
  "UIUC Releases" at "http://cogcomp.cs.illinois.edu/m2repo"
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
  //"org.scalautils" % "scalautils_2.11" % "2.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "cc.factorie" % "factorie_2.11" % "1.1",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.5.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.1",
  "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6",
  "org.scala-lang" % "scala-compiler" % "2.11.4",
  "org.sameersingh.htmlgen" % "htmlgen" % "0.3-SNAPSHOT",
  "org.sameersingh.scalaplot" % "scalaplot" % "0.1",
//  "ml.wolfe" %% "wolfe-core" % "0.5.0-SNAPSHOT" exclude("org.slf4j", "slf4j-simple"),
  "ml.wolfe" %% "wolfe-util" % "0.5.0-SNAPSHOT" exclude("org.slf4j", "slf4j-simple"),
  "ml.wolfe" %% "wolfe-examples" % "0.5.0-SNAPSHOT" exclude("org.slf4j", "slf4j-simple"),
  "ml.wolfe" %% "wolfe-nlp" % "0.5.0-SNAPSHOT" exclude("org.slf4j", "slf4j-simple"),
  "ml.wolfe" %% "wolfe-ui" % "0.5.0-SNAPSHOT" exclude("org.slf4j", "slf4j-simple"),
  "edu.arizona.sista" % "processors" % "3.3",
  "org.scala-lang" % "scala-library" % "2.11.4",
  "org.pegdown" % "pegdown" % "1.4.2",
  //"ws.securesocial" %% "securesocial" % "3.0-M3" exclude("org.slf4j", "slf4j-simple"),
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
  "jline" % "jline" % "2.12.1"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)