
organization := "org.anized"
name := "linkscore"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.8.0",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.2.0",
  "com.typesafe" % "config" % "1.4.0",
  "com.iheart" %% "ficus" % "1.4.7",

  "com.dimafeng" %% "testcontainers-scala" % "0.33.0" % Test,
  "org.mockito" % "mockito-all" % "2.0.2-beta" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/")

trapExit := false

enablePlugins(DockerPlugin)
dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget
  new Dockerfile {
    from("openjdk:8-jre")
    add(classpath.files, "/app/")
    add(jarFile, jarTarget)
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}
