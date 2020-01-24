
organization := "org.anized"
name := "linkscore"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.8.0",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.2.0",
  "com.typesafe" % "config" % "1.4.0",

  "org.mockito" % "mockito-all" % "2.0.2-beta" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/")

trapExit := false
