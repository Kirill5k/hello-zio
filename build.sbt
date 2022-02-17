import com.typesafe.sbt.packager.docker._

ThisBuild / scalaVersion                        := "2.13.8"
ThisBuild / version                             := scala.sys.process.Process("git rev-parse HEAD").!!.trim.slice(0, 7)
ThisBuild / organization                        := "io.github.kirill5k"
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowJavaVersions          := Seq("amazon-corretto@1.17")

val noPublish = Seq(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false,
  publish / skip  := true
)

val docker = Seq(
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

val domain = project
  .in(file("domain"))
  .settings(noPublish)
  .settings(
    name       := "hello-zio-domain",
    moduleName := "domain",
    libraryDependencies ++= Dependencies.domain
  )

val consumer = project
  .in(file("consumer"))
  .dependsOn(domain % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  .settings(docker)
  .settings(
    name       := "hello-zio-consumer",
    moduleName := "hello-zio-consumer",
    libraryDependencies ++= Dependencies.consumer ++ Dependencies.test,
    addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )

val api = project
  .in(file("api"))
  .dependsOn(domain % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  .settings(docker)
  .settings(
    name       := "hello-zio-api",
    moduleName := "hello-zio-api",
    libraryDependencies ++= Dependencies.api ++ Dependencies.test,
    addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )

val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    name := "hello-zio"
  )
  .aggregate(consumer, api, domain)
