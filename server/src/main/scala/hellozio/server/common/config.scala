package hellozio.server.common

import hellozio.server.common.errors.AppError
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.Has
import zio.ZIO
import zio.ZLayer
import zio.blocking.Blocking
import zio.blocking.blocking

object config {

  final case class ServerConfig(
      host: String,
      port: Int
  )

  final case class KafkaConfig(
      bootstrapServers: String
  )

  final case class AppConfig(
      server: ServerConfig,
      kafka: KafkaConfig
  )

  object AppConfig {

    val layer: ZLayer[Blocking, AppError, Has[AppConfig]] =
      blocking(ZIO.effect(ConfigSource.default.load[AppConfig]))
        .flatMap { result =>
          ZIO.fromEither(result).mapError(e => AppError.ConfigError(e.head.description))
        }
        .orDie
        .toLayer

  }

}
