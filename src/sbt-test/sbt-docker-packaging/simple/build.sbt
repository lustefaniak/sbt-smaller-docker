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
