package hellozio.api.todo

import hellozio.api.common.config.AppConfig
import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{Todo, TodoUpdate}
import hellozio.domain.common.kafka.Serde
import io.circe.generic.auto._
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Has
import zio.IO
import zio.URLayer
import zio.ZIO
import zio.blocking.Blocking
import zio.kafka.producer.Producer
import zio.kafka.producer.ProducerSettings

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
        Serde.todoId,
        Serde.json[TodoUpdate]
      )
      .mapError(e => AppError.KafkaError(e.getMessage))
      .unit
}

object TodoPublisher {

  val live: URLayer[Has[AppConfig] with Blocking, Has[TodoPublisher]] =
    ZIO
      .access[Has[AppConfig]](_.get)
      .toManaged_
      .flatMap { config =>
        Producer
          .make(ProducerSettings(List(config.kafka.bootstrapServers)))
          .map(p => TodoPublisherLive(p, config.kafka.topic))
      }
      .orDie
      .toLayer

}
