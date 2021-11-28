package hellozio.server.todo

import hellozio.server.common.config.AppConfig
import hellozio.server.common.errors.AppError
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Has
import zio.IO
import zio.URLayer
import zio.ZIO
import zio.blocking.Blocking
import zio.kafka.producer.Producer
import zio.kafka.producer.ProducerSettings
import zio.kafka.serde.Serde

trait TodoPublisher {
  def send(update: TodoUpdate): IO[AppError, Unit]
}

final private case class TodoPublisherLive(
    producer: Producer,
    topic: String
) extends TodoPublisher {

  override def send(update: TodoUpdate): IO[AppError, Unit] =
      producer
        .produce(
          new ProducerRecord[Todo.Id, TodoUpdate](topic, update.id, update),
          TodoPublisher.todoIdSerde,
          TodoPublisher.todoUpdateSerde
        )
        .mapError(e => AppError.KafkaError(e.getMessage))
        .unit

}

object TodoPublisher {

  val todoIdSerde: Serde[Any, Todo.Id] = Serde.string.inmap(Todo.Id.apply)(_.value)

  val todoUpdateSerde: Serde[Any, TodoUpdate] =
    Serde
      .string
      .inmapM(json => ZIO.fromEither(decode[TodoUpdate](json)))(todo => ZIO.effect(todo.asJson.noSpaces))

  val live: URLayer[Has[AppConfig] with Blocking, Has[TodoPublisher]] =
    ZIO
      .access[Has[AppConfig]](_.get)
      .toManaged_
      .flatMap { config =>
        val settings = ProducerSettings(List(config.kafka.bootstrapServers))
        Producer.make(settings).map(p => TodoPublisherLive(p, config.kafka.topic))
      }
      .orDie
      .toLayer

}
