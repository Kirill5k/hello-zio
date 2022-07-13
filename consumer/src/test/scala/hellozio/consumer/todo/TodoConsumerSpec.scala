package hellozio.consumer.todo

import hellozio.consumer.common.config.{AppConfig, KafkaConfig}
import hellozio.domain.todo.{TodoUpdate, Todos}
import io.circe.syntax.*
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}
import zio.*
import zio.test.Assertion.*
import zio.test.*

object TodoConsumerSpec extends ZIOSpecDefault {

  val topic     = "todo-updates"
  val kafkaPort = 29095
  val appConfig = AppConfig(KafkaConfig(s"localhost:$kafkaPort", "todo-consumer", topic))

  given Serializer[String]   = new StringSerializer()
  given Deserializer[String] = new StringDeserializer()
  given EmbeddedKafkaConfig  = EmbeddedKafkaConfig(kafkaPort = kafkaPort)

  def spec = suite("A TodoConsumer should")(
    test("consume todo updates") {
      for
        _ <- ZIO.acquireRelease(ZIO.attempt(EmbeddedKafka.start()))(s => ZIO.attempt(EmbeddedKafka.stop(s)).orDie)
        _ <- TestClock.adjust(1.second)
        fib <- TodoConsumer.updates
          .interruptAfter(10.seconds)
          .runCollect
          .map(_.toList)
          .provide(ZLayer.succeed(appConfig), ZLayer.succeed(Clock.ClockLive), TodoConsumer.layer)
          .fork
        todo = TodoUpdate.Created(Todos.id, Todos.todo)
        _   <- ZIO.attempt(EmbeddedKafka.publishToKafka(new ProducerRecord(topic, Todos.id.value, todo.asJson.noSpaces)))
        res <- fib.join
      yield assert(res)(equalTo(List(todo)))
    } @@ TestAspect.ignore
  )
}
