package net.globalwebindex.sbt.docker

import java.io.File

import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.docker.{ CmdLike, DockerPlugin, Dockerfile, ExecCmd }
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._

object SmallerDockerPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = DockerPlugin

  object autoImport {

    def smallerDockerSettings(isFrequentlyChangingFile: sbt.File => Boolean): Seq[sbt.Setting[_]] = Seq(
      publishLocal in Docker := {
        val stagingDir = (stage in Docker).value
        val s          = (streams in Docker).value

        val dockerCommands = SmallerDocker.generateDockerCommands(isFrequentlyChangingFile)(
          (dockerBaseImage in Docker).value,
          (maintainer in Docker).value,
          (defaultLinuxInstallLocation in Docker).value,
          (daemonUser in Docker).value,
          (daemonGroup in Docker).value,
          (dockerExposedPorts in Docker).value,
          (dockerExposedUdpPorts in Docker).value,
          (dockerExposedVolumes in Docker).value,
          stagingDir,
          (dockerCmd in Docker).value,
          (dockerEntrypoint in Docker).value
        )
        generateDockerConfig(dockerCommands, stagingDir) //It is here to make sure Docker file is regenerated after all dependencies were staged

        DockerPlugin.publishLocalDocker(stagingDir, (dockerBuildCommand in Docker).value, s.log)
      },
      dockerCommands := Seq(ExecCmd("THIS WILL BE OVERRIDDEN DURING PUBLISHLOCAL TASK!"))
    )
  }

  private[this] def generateDockerConfig(commands: Seq[CmdLike], target: File): File = {
    val dockerContent = Dockerfile(commands: _*).makeContent
    val f             = target / "Dockerfile"
    IO.write(f, dockerContent)
    f
  }

}
