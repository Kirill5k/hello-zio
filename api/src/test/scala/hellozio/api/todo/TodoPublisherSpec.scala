package hellozio.api.todo

import io.circe.parser._
import io.circe.generic.auto._
import hellozio.api.common.config.{AppConfig, KafkaConfig, ServerConfig}
import hellozio.domain.todo.{TodoUpdate, Todos}
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.{Clock, Runtime, Unsafe, ZIO, ZLayer}

class TodoPublisherSpec extends AnyWordSpec with Matchers with EmbeddedKafka {

  val topic     = "todo-updates"
  val kafkaPort = 29092
  val appConfig = AppConfig(ServerConfig("0.0.0.0", 8080), KafkaConfig(s"localhost:$kafkaPort", topic))

  "A TodoPublisher" should {

    "publish todo updates to a topic" in {
      implicit val config = EmbeddedKafkaConfig(kafkaPort = kafkaPort)
      withRunningKafka {
        val updates = List(
          TodoUpdate.Created(Todos.id, Todos.todo),
          TodoUpdate.Updated(Todos.id, Todos.todo),
          TodoUpdate.Deleted(Todos.id)
        )

        run(
          ZIO
            .foreach(updates)(u => TodoPublisher.send(u))
            .provide(ZLayer.succeed(appConfig), TodoPublisher.layer, ZLayer.succeed(Clock.ClockLive))
        )

        val msgs = consumeNumberStringMessagesFrom(topic, 3)
        msgs.flatMap(decode[TodoUpdate](_).toOption) mustBe updates
      }
    }
  }

  def run[E, A](zio: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(zio).getOrThrowFiberFailure()
    }
}
