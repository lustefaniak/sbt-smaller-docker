package net.globalwebindex.sbt.docker

import com.typesafe.sbt.packager.docker.{ Cmd, CmdLike, ExecCmd }
import sbt._

import scala.collection.breakOut
/*
Based on code from DockerPlugin, unfortunatelly helper methods are private so there is no way to reuse them
 */
object SmallerDocker {

  def generateDockerCommands(isFrequentlyChangingFile: sbt.File => Boolean)(baseImage: String,
                                                                            maintainer: String,
                                                                            dockerBaseDirectory: String,
                                                                            user: String,
                                                                            group: String,
                                                                            exposedPorts: Seq[Int],
                                                                            udpPorts: Seq[Int],
                                                                            exposedVolumes: Seq[String],
                                                                            dockerStaging: sbt.File,
                                                                            cmd: Seq[String],
                                                                            entrypoint: Seq[String]): Seq[CmdLike] = {

    val generalCommands = makeFrom(baseImage) +: makeMaintainer(maintainer).toSeq

    generalCommands ++ Seq(makeWorkdir(dockerBaseDirectory)) ++ makeExposePorts(exposedPorts, udpPorts) ++
      makeVolumes(exposedVolumes, user, group) ++ makeAdds(dockerBaseDirectory, dockerStaging, isFrequentlyChangingFile) ++
      Seq(
        makeUser(user),
        makeEntrypoint(entrypoint),
        makeCmd(cmd)
      )

  }

  private final def makeMaintainer(maintainer: String): Option[CmdLike] =
    if (maintainer.isEmpty) None else Some(makeLabel(Tuple2("MAINTAINER", maintainer)))
  private final def makeLabel(label: (String, String)): CmdLike = {
    val (variable, value) = label
    Cmd("LABEL", variable + "=\"" + value.toString + "\"")
  }
  private final def makeFrom(dockerBaseImage: String): CmdLike        = Cmd("FROM", dockerBaseImage)
  private final def makeWorkdir(dockerBaseDirectory: String): CmdLike = Cmd("WORKDIR", dockerBaseDirectory)

  private final def fileSize(file: sbt.File): Long =
    file.getAbsoluteFile.length()
  private final def makeAdds(dockerBaseDirectory: String, dockerStaging: sbt.File, isFrequentlyChangingFile: sbt.File => Boolean): Seq[CmdLike] = {
    def dockerPath(file: sbt.File): String =
      file.absolutePath.stripPrefix(dockerStaging.absolutePath).stripPrefix("/")
    def getRecursiveListOfFiles(dir: File): Array[File] = {
      val these = dir.listFiles
      these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
    }

    val filesInStaging                    = getRecursiveListOfFiles(dockerStaging).toVector
    val directoriesToCreate: List[String] = filesInStaging.filter(_.isDirectory).map(dockerPath).filterNot(_.isEmpty).map("/" + _)(breakOut)

    val (changingFrequently, notChangingOften) =
      filesInStaging.filter(_.isFile).filterNot(_.name.contains("Dockerfile")).sortBy(fileSize)(Ordering.Long.reverse).partition(isFrequentlyChangingFile)

    val createDirectoriesCommand = if (directoriesToCreate.nonEmpty) Seq(Cmd("RUN", (Seq("mkdir", "-p") ++ directoriesToCreate): _*)) else Seq.empty

    createDirectoriesCommand ++ createAddCommands(notChangingOften.map(dockerPath)) ++ createAddCommands(changingFrequently.map(dockerPath))
  }
  private final def createAddCommands(files: Seq[String]): Seq[CmdLike] = {
    val groupedByDirectory = files.groupBy(f => s"/${f.split("/").dropRight(1).mkString("/")}/")

    groupedByDirectory.map {
      case (directory, files) =>
        Cmd("ADD", (files.sorted ++ Seq(directory)): _*)
    }(breakOut)
  }
  private final def makeChown(daemonUser: String, daemonGroup: String, directories: Seq[String]): CmdLike =
    ExecCmd("RUN", Seq("chown", "-R", s"$daemonUser:$daemonGroup") ++ directories: _*)
  private final def makeUser(daemonUser: String): CmdLike            = Cmd("USER", daemonUser)
  private final def makeEntrypoint(entrypoint: Seq[String]): CmdLike = ExecCmd("ENTRYPOINT", entrypoint: _*)
  private final def makeCmd(args: Seq[String]): CmdLike              = ExecCmd("CMD", args: _*)
  private final def makeExposePorts(exposedPorts: Seq[Int], exposedUdpPorts: Seq[Int]): Option[CmdLike] =
    if (exposedPorts.isEmpty && exposedUdpPorts.isEmpty) None
    else Some(Cmd("EXPOSE", (exposedPorts.map(_.toString) ++ exposedUdpPorts.map(_.toString).map(_ + "/udp")) mkString " "))
  private final def makeVolumes(exposedVolumes: Seq[String], daemonUser: String, daemonGroup: String): Seq[CmdLike] =
    if (exposedVolumes.isEmpty) Seq.empty
    else
      Seq(
        ExecCmd("RUN", Seq("mkdir", "-p") ++ exposedVolumes: _*),
        makeChown(daemonUser, daemonGroup, exposedVolumes),
        ExecCmd("VOLUME", exposedVolumes: _*)
      )

}
