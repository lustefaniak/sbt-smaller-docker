version := "0.1"
organization := "com.example"
name := "simple"
scalaVersion := "2.12.6"

def isFrequentlyChangingFile(file: sbt.File): Boolean = {
  val fileName = file.name
  if (fileName.startsWith("com.example")) true
  else if (fileName.endsWith(".jar")) false
  else true
}

enablePlugins(JavaAppPackaging, SmallerDockerPlugin)
smallerDockerSettings(isFrequentlyChangingFile)

TaskKey[Unit]("check") := {
  val stage = (stagingDirectory in Docker).value
  val lines = IO.readLines(stage / "Dockerfile").map(_.trim)

  def check(str:String):Unit = {
    assert(lines.contains(str), s"${str} not found int ${lines}")

  }

  check("ADD opt/docker/lib/org.scala-lang.scala-library-2.12.6.jar /opt/docker/lib/")
  check("ADD opt/docker/lib/com.example.simple-0.1.jar /opt/docker/lib/")
}
