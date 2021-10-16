//import com.typesafe.sbt.packager.docker._

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := scala.sys.process.Process("git rev-parse HEAD").!!.trim.slice(0, 7)
ThisBuild / organization := "io.github.kirill5k"
//ThisBuild / githubWorkflowPublishTargetBranches := Seq()

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publish / skip := true
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
//  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
//  .settings(docker)
  .settings(
    name := "hello-zio-client",
    moduleName := "client"
  )

lazy val server = project
  .in(file("server"))
  //  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  //  .settings(docker)
  .settings(
    name := "hello-zio-server",
    moduleName := "server"
  )