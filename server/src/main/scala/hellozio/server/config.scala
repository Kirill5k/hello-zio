package hellozio.server

import hellozio.server.errors.AppError
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.{Has, Layer, ZIO}

object config {

  final case class ServerConfig(
      host: String,
      port: Int
  )

  final case class AppConfig(
      server: ServerConfig
  )

  object AppConfig {
    val live: Layer[AppError, Has[AppConfig]] =
      ZIO
        .effect(ConfigSource.default.load[AppConfig])
        .flatMap {
          case Right(config) => ZIO.succeed(config)
          case Left(failure) => ZIO.fail(AppError.ConfigError(failure.head.description))
        }
        .orDie
        .toLayer
  }

}
