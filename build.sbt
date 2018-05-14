name := "sbt-smaller-docker"
organization := "net.globalwebindex"
homepage := Some(url(s"https://github.com/GlobalWebIndex/${name.value}/blob/master/README.md"))
licenses := Seq("MIT" -> url(s"https://github.com/GlobalWebIndex/${name.value}/blob/v${version.value}/LICENSE"))
description := "package publisher for bintray.com"
developers := List(
  Developer("lustefaniak", "Lukas Stefaniak", "@lustefaniak", url("https://github.com/lustefaniak"))
)
scmInfo := Some(ScmInfo(url(s"https://github.com/GlobalWebIndex/${name.value}"), s"git@github.com:GlobalWebIndex/${name.value}.git"))

sbtPlugin := true
crossSbtVersions := Seq("0.13.16", "1.1.5")
enablePlugins(DynVerPlugin)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")

publishMavenStyle := false
bintrayPackageLabels := Seq("sbt", "plugin")
bintrayRepository := "sbt-plugins"
bintrayVcsUrl := Some("git@github.com:GlobalWebIndex/sbt-smaller-docker.git")

initialCommands in console := "import net.globalwebindex.sbt.docker._"

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

cancelable in Global := true
