import sbt._

object Dependencies {

  private object Versions {
    val circe          = "0.14.3"
    val logback        = "1.4.1"
    val pureconfig     = "0.17.1"
    val tapir          = "1.0.0-RC1"
    val zio            = "2.0.2"
    val zioInteropCats = "3.3.0"
    val fs2Kafka       = "3.0.0-M7"
    val kafka          = "3.2.0"
    val http4s         = "0.23.12"
    val mongo4cats     = "0.6.3"

    val scalaTest = "3.2.12"
    val mockito   = "3.2.10.0"
  }

  private object Libraries {
    val pureconfig = "com.github.pureconfig" %% "pureconfig-core" % Versions.pureconfig
    val logback    = "ch.qos.logback"         % "logback-classic" % Versions.logback
    val fs2Kafka   = "com.github.fd4s"       %% "fs2-kafka"       % Versions.fs2Kafka

    object mongo4cats {
      val zio      = "io.github.kirill5k" %% "mongo4cats-zio"          % Versions.mongo4cats
      val circe    = "io.github.kirill5k" %% "mongo4cats-circe"        % Versions.mongo4cats
      val embedded = "io.github.kirill5k" %% "mongo4cats-zio-embedded" % Versions.mongo4cats % Test

      val all = Seq(zio, circe, embedded)
    }

    object http4s {
      val blazeServer = "org.http4s" %% "http4s-blaze-server" % Versions.http4s
      val circe       = "org.http4s" %% "http4s-circe"        % Versions.http4s

      val all = Seq(blazeServer, circe)
    }

    object tapir {
      val core      = "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir
      val circe     = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir
      val ziohttp4s = "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % Versions.tapir

      val all = Seq(core, circe, zio, ziohttp4s)
    }

    object circe {
      val core    = "io.circe" %% "circe-core"    % Versions.circe
      val generic = "io.circe" %% "circe-generic" % Versions.circe
      val parser  = "io.circe" %% "circe-parser"  % Versions.circe

      val all = Seq(core, generic, parser)
    }

    val zio            = "dev.zio" %% "zio"              % Versions.zio
    val zioStreams     = "dev.zio" %% "zio-streams"      % Versions.zio
    val zioTest        = "dev.zio" %% "zio-test"         % Versions.zio % Test
    val zioTestSbt     = "dev.zio" %% "zio-test-sbt"     % Versions.zio % Test
    val zioInteropCats = "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats

    val mockito       = "org.scalatestplus"       %% "mockito-3-4"    % Versions.mockito % Test
    val embeddedkafka = "io.github.embeddedkafka" %% "embedded-kafka" % Versions.kafka   % Test
  }

  val api = Seq(
    Libraries.pureconfig,
    Libraries.logback,
    Libraries.http4s.blazeServer
  ) ++
    Libraries.tapir.all ++
    Libraries.mongo4cats.all

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
    Libraries.mockito,
    Libraries.embeddedkafka.cross(CrossVersion.for3Use2_13),
    Libraries.zioTest,
    Libraries.zioTestSbt
  )
}
