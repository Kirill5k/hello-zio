package hellozio.server

import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion.equalTo
import zio.test.mock.Expectation.value
import zio.test.mock.{MockClock, MockRandom}
import zio.test.{DefaultRunnableSpec, assertM}
import zio.{Has, ULayer}

import java.time.OffsetDateTime

class GreeterSpec extends DefaultRunnableSpec  {
  val dateTime = OffsetDateTime.parse("2021-01-01T00:00:00Z")

  def spec = suite("greet")(
    testM("greets a visitor with a random greeting") {
      val clock = MockClock.CurrentDateTime(value(dateTime))
      val random = MockRandom.NextIntBounded(equalTo(4), value(1))
      val mockEnv: ULayer[Has[Random.Service] with Has[Clock.Service]] = random ++ clock
      val result = Greeter.greet("Boris").provideLayer(mockEnv >>> Greeter.layer)
      assertM(result)(equalTo(Greeting("Bonjour", "Boris", dateTime)))
    }
  )
}
