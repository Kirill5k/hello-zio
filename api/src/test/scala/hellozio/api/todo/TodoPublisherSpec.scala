package hellozio.api.todo

import io.circe.parser._
import io.circe.generic.auto._
import hellozio.api.common.config.{AppConfig, KafkaConfig, ServerConfig}
import hellozio.domain.todo.{TodoUpdate, Todos}
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.ZLayer
import zio.blocking.Blocking
import zio.Runtime

class TodoPublisherSpec extends AnyWordSpec with Matchers with EmbeddedKafka {

  val topic     = "todo-updates"
  val kafkaPort = 29092
  val appConfig = AppConfig(ServerConfig("0.0.0.0", 8080), KafkaConfig(s"localhost:$kafkaPort", topic))

  val layer = (ZLayer.succeed(appConfig) ++ Blocking.live) >>> TodoPublisher.layer

  "A TodoPublisher" should {

    "publish todo updates to a topic" in {
      implicit val config = EmbeddedKafkaConfig(kafkaPort = kafkaPort)
      withRunningKafka {
        val update = TodoUpdate.Created(Todos.id, Todos.todo)
        Runtime.default.unsafeRunSync(TodoPublisher(_.send(update)).provideLayer(layer))

        val msg = consumeFirstStringMessageFrom(topic)
        decode[TodoUpdate](msg) mustBe Right(update)
      }
    }
  }
}
