import com.typesafe.sbt.packager.docker._

ThisBuild / scalaVersion                        := "2.13.6"
ThisBuild / version                             := scala.sys.process.Process("git rev-parse HEAD").!!.trim.slice(0, 7)
ThisBuild / organization                        := "io.github.kirill5k"
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

lazy val noPublish = Seq(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false,
  publish / skip  := true
)

lazy val docker = Seq(
  packageName        := moduleName.value,
  version            := version.value,
  maintainer         := "immotional@aol.com",
  dockerBaseImage    := "adoptopenjdk/openjdk16-openj9:x86_64-alpine-jre-16_36_openj9-0.25.0",
  dockerUpdateLatest := true,
  makeBatScripts     := List(),
  dockerCommands := {
    val commands         = dockerCommands.value
    val (stage0, stage1) = commands.span(_ != DockerStageBreak)
    val (before, after)  = stage1.splitAt(4)
    val installBash      = Cmd("RUN", "apk update && apk upgrade && apk add bash")
    stage0 ++ before ++ List(installBash) ++ after
  }
)

lazy val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    name := "hello-zio"
  )
  .aggregate(client, server)

lazy val client = project
  .in(file("client"))
  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  .settings(docker)
  .settings(
    name       := "hello-zio-client",
    moduleName := "client",
    libraryDependencies ++= Dependencies.client
  )

lazy val server = project
  .in(file("server"))
  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  .settings(docker)
  .settings(
    name       := "hello-zio-server",
    moduleName := "server",
    libraryDependencies ++= Dependencies.server,
    addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )
