package hellozio.consumer.todo

import fs2.Stream
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}
import hellozio.consumer.common.config.AppConfig
import hellozio.domain.common.errors.AppError
import hellozio.domain.common.kafka.Serde
import hellozio.domain.todo.{Todo, TodoUpdate}
import io.circe.generic.auto._
import zio.interop.catz._
import zio.stream.ZStream
import zio.stream.interop.fs2z._
import zio._

trait TodoConsumer {
  def updates: ZStream[Clock, AppError, TodoUpdate]
}

final private case class TodoConsumerLive(
    consumer: KafkaConsumer[RIO[Clock, *], Todo.Id, TodoUpdate],
    topic: String
) extends TodoConsumer {

  override def updates: ZStream[Clock, AppError, TodoUpdate] = {
    Stream
      .eval(consumer.subscribeTo(topic))
      .flatMap(_ => consumer.stream)
      .toZStream()
      .mapZIO(c => c.offset.commit.as(c.record.value))
      .mapError(e => AppError.Kafka(e.getMessage))
  }
}

object TodoConsumer {
  lazy val layer: URLayer[AppConfig with Clock, TodoConsumer] = ZIO
    .serviceWith[AppConfig](_.kafka)
    .toManaged
    .flatMap { config =>
      val settings = ConsumerSettings(
        keyDeserializer = Serde.todoIdDeserializer,
        valueDeserializer = Serde.jsonDeserializer[TodoUpdate]
      ).withAutoOffsetReset(AutoOffsetReset.Latest)
        .withBootstrapServers(config.bootstrapServers)
        .withGroupId(config.groupId)
      KafkaConsumer
        .resource(settings)
        .toManagedZIO
        .map(c => TodoConsumerLive(c, config.topic))
    }
    .tapZIO(_ => Console.printLine(("-" * 20) + " TERMINATING! " + ("-" * 20)).provideLayer(Console.live))
    .orDie
    .toLayer

  def updates: ZStream[TodoConsumer with Clock, AppError, TodoUpdate] = ZStream.serviceWithStream[TodoConsumer](_.updates)
}
