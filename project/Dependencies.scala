import sbt._

object Dependencies {

  private object Versions {
    val circe      = "0.14.1"
    val logback    = "1.2.7"
    val pureconfig = "0.17.0"
    val sttp       = "3.3.16"
    val tapir      = "0.19.0"
    val zio        = "1.0.12"
    val zioKafka   = "0.17.1"

    val scalaTest = "3.2.10"
    val mockito   = "3.2.10.0"
  }

  private object Libraries {
    val pureconfig = "com.github.pureconfig" %% "pureconfig"      % Versions.pureconfig
    val logback    = "ch.qos.logback"         % "logback-classic" % Versions.logback

    object tapir {
      val core      = "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir
      val circe     = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir
      val ziohttp4s = "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % Versions.tapir

      val all = Seq(core, circe, zio, ziohttp4s)
    }

    object circe {
      val core    = "io.circe" %% "circe-core"           % Versions.circe
      val generic = "io.circe" %% "circe-generic"        % Versions.circe
      val extras  = "io.circe" %% "circe-generic-extras" % Versions.circe
      val parser  = "io.circe" %% "circe-parser"         % Versions.circe

      val all = Seq(core, generic, parser, extras)
    }

    object sttp {
      val core    = "com.softwaremill.sttp.client3" %% "core"                          % Versions.sttp
      val backend = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % Versions.sttp
      val circe   = "com.softwaremill.sttp.client3" %% "circe"                         % Versions.sttp

      val all = Seq(core, backend, circe)
    }

    val zio        = "dev.zio" %% "zio"         % Versions.zio
    val zioStreams = "dev.zio" %% "zio-streams" % Versions.zio
    val zioKafka   = "dev.zio" %% "zio-kafka"   % Versions.zioKafka

    val scalaTest = "org.scalatest"     %% "scalatest"   % Versions.scalaTest % Test
    val mockito   = "org.scalatestplus" %% "mockito-3-4" % Versions.mockito   % Test
  }

  lazy val api = Seq(
    Libraries.pureconfig,
    Libraries.logback
  ) ++
    Libraries.tapir.all

  lazy val consumer = Seq(
    Libraries.pureconfig,
    Libraries.logback
  )

  lazy val domain = Seq(
    Libraries.zio,
    Libraries.zioStreams,
    Libraries.zioKafka
  ) ++
    Libraries.circe.all

  lazy val test = Seq(
    Libraries.scalaTest,
    Libraries.mockito
  )
}
