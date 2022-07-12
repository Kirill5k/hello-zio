package hellozio.consumer.todo

import fs2.Stream
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}
import hellozio.consumer.common.config.AppConfig
import hellozio.domain.common.errors.AppError
import hellozio.domain.common.kafka.Serde
import hellozio.domain.todo.{Todo, TodoUpdate}
import zio.interop.catz.*
import zio.stream.ZStream
import zio.stream.interop.fs2z.*
import zio.*

trait TodoConsumer {
  def updates: ZStream[Any, AppError, TodoUpdate]
}

final private case class TodoConsumerLive(
    consumer: KafkaConsumer[RIO[Clock, *], Todo.Id, TodoUpdate],
    clock: Clock,
    topic: String
) extends TodoConsumer {

  override def updates: ZStream[Any, AppError, TodoUpdate] =
    Stream
      .eval(consumer.subscribeTo(topic))
      .flatMap(_ => consumer.stream)
      .toZStream()
      .mapZIO(c => c.offset.commit.as(c.record.value))
      .mapError(e => AppError.Kafka(e.getMessage))
      .provideLayer(ZLayer.succeed(clock))
}

object TodoConsumer {
  lazy val layer: URLayer[AppConfig with Clock, TodoConsumer] =
    ZLayer.scoped {
      ZIO
        .serviceWith[AppConfig](_.kafka)
        .zip(ZIO.service[Clock])
        .flatMap { (config, clock) =>
          val settings = ConsumerSettings(
            keyDeserializer = Serde.todoIdDeserializer,
            valueDeserializer = Serde.jsonDeserializer[TodoUpdate]
          ).withAutoOffsetReset(AutoOffsetReset.Latest)
            .withBootstrapServers(config.bootstrapServers)
            .withGroupId(config.groupId)
          KafkaConsumer
            .resource(settings)
            .toScopedZIO
            .map(c => TodoConsumerLive(c, clock, config.topic))
        }
        .orDie
    }

  def updates: ZStream[TodoConsumer, AppError, TodoUpdate] = ZStream.serviceWithStream[TodoConsumer](_.updates)
}
