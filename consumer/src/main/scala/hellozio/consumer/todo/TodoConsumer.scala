package hellozio.consumer.todo

import hellozio.consumer.common.config.AppConfig
import hellozio.domain.common.errors.AppError
import hellozio.domain.common.kafka.Serde
import hellozio.domain.todo.TodoUpdate
import io.circe.generic.auto._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.{Has, Queue, URLayer, ZIO}
import zio.stream.ZStream

trait TodoConsumer {
  def updates: ZStream[Clock, AppError, TodoUpdate]
}

final private case class TodoConsumerLive(
    consumer: Consumer,
    topic: String
) extends TodoConsumer {

  override def updates: ZStream[Clock, AppError, TodoUpdate] =
    ZStream
      .fromEffect(Queue.bounded[TodoUpdate](1024))
      .flatMap { queue =>
        val events = consumer
          .subscribeAnd(Subscription.topics(topic))
          .plainStream(Serde.todoId, Serde.json[TodoUpdate])
          .mapM(r => queue.offer(r.value).as(r.offset))
          .aggregateAsync(Consumer.offsetBatches)
          .mapM(_.commit)
          .mapError(e => AppError.Kafka(e.getMessage))

        ZStream
          .fromQueue(queue)
          .drainFork(events)
      }
}

object TodoConsumer {

  lazy val live: URLayer[Has[AppConfig] with Blocking with Clock, Has[TodoConsumer]] = ZIO
    .access[Has[AppConfig]](_.get.kafka)
    .toManaged_
    .flatMap { kafka =>
      Consumer
        .make(ConsumerSettings(List(kafka.bootstrapServers)).withGroupId(kafka.groupId))
        .map(c => TodoConsumerLive(c, kafka.topic))
    }
    .orDie
    .toLayer

  def updates: ZStream[Has[TodoConsumer] with Clock, AppError, TodoUpdate] = ZStream.service[TodoConsumer].flatMap(_.updates)
}
