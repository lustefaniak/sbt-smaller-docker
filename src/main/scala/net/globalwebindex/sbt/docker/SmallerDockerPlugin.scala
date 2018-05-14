package net.globalwebindex.sbt.docker

import java.io.File

import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.Keys.stagingDirectory
import com.typesafe.sbt.packager.Stager
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.docker.{ CmdLike, DockerPlugin, Dockerfile }
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._

object SmallerDockerPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = DockerPlugin

  object autoImport {

    val dockerStageFiles = TaskKey[File]("docker-stage-files", "Stage all docker image files except Dockerfile.")

    def smallerDockerSettings(isFrequentlyChangingFile: sbt.File => Boolean): Seq[sbt.Setting[_]] =
      inConfig(Docker)(
        Seq(
          dockerStageFiles := Stager.stage(Docker.name)(streams.value, stagingDirectory.value, mappings.value),
          stage := {
            val staged = dockerStageFiles.value
            val generatedDockerfile = dockerGenerateConfig.value
            staged
          }
        )
      ) ++ Seq(
        dockerCommands := {
          SmallerDocker.generateDockerCommands(isFrequentlyChangingFile)(
            (dockerBaseImage in Docker).value,
            (maintainer in Docker).value,
            (defaultLinuxInstallLocation in Docker).value,
            (daemonUser in Docker).value,
            (daemonGroup in Docker).value,
            (dockerExposedPorts in Docker).value,
            (dockerExposedUdpPorts in Docker).value,
            (dockerExposedVolumes in Docker).value,
            (dockerStageFiles in Docker).value,
            (dockerCmd in Docker).value,
            (dockerEntrypoint in Docker).value
          )
        }
      )
  }

  private[this] def generateDockerConfig(commands: Seq[CmdLike], target: File): File = {
    val dockerContent = Dockerfile(commands: _*).makeContent
    val f             = target / "Dockerfile"
    IO.write(f, dockerContent)
    f
  }

}
