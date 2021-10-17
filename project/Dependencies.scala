import sbt._

object Dependencies {

  private object Versions {
    val pureconfig = "0.17.0"
    val sttp       = "3.3.15"
    val zio        = "1.0.12"
    val zioHttp    = "1.0.0.0-RC17"

    val scalaTest = "3.2.10"
    val mockito   = "3.2.10.0"
  }

  private object Libraries {
    val pureconfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureconfig

    object sttp {
      val core    = "com.softwaremill.sttp.client3" %% "core"                          % Versions.sttp
      val backend = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % Versions.sttp
      val circe   = "com.softwaremill.sttp.client3" %% "circe"                         % Versions.sttp
    }

    val zio        = "dev.zio" %% "zio"          % Versions.zio
    val zioTest    = "dev.zio" %% "zio-test"     % Versions.zio % Test
    val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Versions.zio % Test

    val zioHttp     = "io.d11" %% "zhttp"      % Versions.zioHttp
    val zioHttpTest = "io.d11" %% "zhttp-test" % Versions.zioHttp % Test

    val scalaTest = "org.scalatest"     %% "scalatest"   % Versions.scalaTest % Test
    val mockito   = "org.scalatestplus" %% "mockito-3-4" % Versions.mockito % Test
  }

  lazy val server = Seq(
    Libraries.pureconfig,
    Libraries.zio,
    Libraries.zioTest,
    Libraries.zioTestSbt,
    Libraries.zioHttp,
    Libraries.scalaTest,
    Libraries.mockito
  )

  lazy val client = Seq(
    Libraries.zio,
    Libraries.sttp.core,
    Libraries.sttp.backend,
    Libraries.sttp.circe
  )

}
