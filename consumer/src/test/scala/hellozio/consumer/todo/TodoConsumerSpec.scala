package hellozio.consumer.todo

import hellozio.consumer.common.config.{AppConfig, KafkaConfig}
import io.github.embeddedkafka.EmbeddedKafka
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TodoConsumerSpec extends AnyWordSpec with Matchers with EmbeddedKafka {

  val topic     = "todo-updates"
  val kafkaPort = 29091
  val appConfig = AppConfig(KafkaConfig(s"localhost:$kafkaPort", "todo-consumer", topic))

  "A TodoConsumer" should {

  }
}
