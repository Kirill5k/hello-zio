package hellozio.api.common

import hellozio.domain.common.errors.AppError
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
      bootstrapServers: String,
      topic: String
  )

  final case class AppConfig(
      server: ServerConfig,
      kafka: KafkaConfig
  )

  object AppConfig {

    lazy val layer: ZLayer[Blocking, AppError, Has[AppConfig]] =
      blocking(ZIO.effect(ConfigSource.default.load[AppConfig]))
        .flatMap { result =>
          ZIO.fromEither(result).mapError(e => AppError.ConfigError(e.head.description))
        }
        .orDie
        .toLayer

  }

}