import sbt._

object Dependencies {

  private object Versions {
    val circe          = "0.14.2"
    val logback        = "1.2.11"
    val pureconfig     = "0.17.1"
    val tapir          = "1.0.0-RC1"
    val zio            = "2.0.0"
    val zioInteropCats = "3.3.0"
    val fs2Kafka       = "3.0.0-M7"
    val kafka          = "3.1.0"
    val http4s         = "0.23.12"

    val scalaTest = "3.2.12"
    val mockito   = "3.2.10.0"
  }

  private object Libraries {
    val pureconfig = "com.github.pureconfig" %% "pureconfig"      % Versions.pureconfig
    val logback    = "ch.qos.logback"         % "logback-classic" % Versions.logback
    val fs2Kafka   = "com.github.fd4s"       %% "fs2-kafka"       % Versions.fs2Kafka

    object http4s {
      val core        = "org.http4s" %% "http4s-core"         % Versions.http4s
      val dsl         = "org.http4s" %% "http4s-dsl"          % Versions.http4s
      val server      = "org.http4s" %% "http4s-server"       % Versions.http4s
      val blazeClient = "org.http4s" %% "http4s-blaze-client" % Versions.http4s
      val blazeServer = "org.http4s" %% "http4s-blaze-server" % Versions.http4s
      val circe       = "org.http4s" %% "http4s-circe"        % Versions.http4s

      val all = Seq(core, dsl, server, blazeServer, circe)
    }

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

    val zio            = "dev.zio" %% "zio"              % Versions.zio
    val zioStreams     = "dev.zio" %% "zio-streams"      % Versions.zio
    val zioTest        = "dev.zio" %% "zio-test"         % Versions.zio % Test
    val zioTestSbt     = "dev.zio" %% "zio-test-sbt"     % Versions.zio % Test
    val zioInteropCats = "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats

    val scalaTest     = "org.scalatest"           %% "scalatest"      % Versions.scalaTest % Test
    val mockito       = "org.scalatestplus"       %% "mockito-3-4"    % Versions.mockito   % Test
    val embeddedkafka = "io.github.embeddedkafka" %% "embedded-kafka" % Versions.kafka     % Test
  }

  val api = Seq(
    Libraries.pureconfig,
    Libraries.logback,
    Libraries.http4s.blazeServer
  ) ++
    Libraries.tapir.all

  val consumer = Seq(
    Libraries.pureconfig,
    Libraries.logback
  )

  val domain = Seq(
    Libraries.zio,
    Libraries.zioStreams,
    Libraries.zioInteropCats,
    Libraries.fs2Kafka
  ) ++
    Libraries.circe.all

  val test = Seq(
    Libraries.scalaTest,
    Libraries.mockito,
    Libraries.embeddedkafka,
    Libraries.zioTest,
    Libraries.zioTestSbt
  )
}
