import com.typesafe.sbt.packager.docker._

ThisBuild / scalaVersion                        := "2.13.7"
ThisBuild / version                             := scala.sys.process.Process("git rev-parse HEAD").!!.trim.slice(0, 7)
ThisBuild / organization                        := "io.github.kirill5k"
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowJavaVersions          := Seq("amazon-corretto@1.17")

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
  dockerBaseImage    := "amazoncorretto:17.0.1-alpine",
  dockerUpdateLatest := true,
  makeBatScripts     := Nil,
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
  .aggregate(consumer, api)

lazy val consumer = project
  .in(file("consumer"))
  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  .settings(docker)
  .settings(
    name       := "hello-zio-consumer",
    moduleName := "consumer",
    libraryDependencies ++= Dependencies.consumer,
    addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )

lazy val api = project
  .in(file("api"))
  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  .settings(docker)
  .settings(
    name       := "hello-zio-api",
    moduleName := "api",
    libraryDependencies ++= Dependencies.api,
    addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )
