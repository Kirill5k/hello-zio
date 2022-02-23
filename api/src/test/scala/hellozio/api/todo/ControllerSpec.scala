package hellozio.api.todo

import org.http4s.{Response, Status}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.parser._
import zio.interop.catz._
import zio._

trait ControllerSpec extends AnyWordSpec with Matchers {

  def verifyJsonResponse(
      actual: Task[Response[Task]],
      expectedStatus: Status,
      expectedBody: Option[String] = None
  ): Assertion = {
    val actualResp = Runtime.default.unsafeRun(actual)

    actualResp.status must be(expectedStatus)
    expectedBody match {
      case Some(expected) =>
        val actual = Runtime.default.unsafeRun(actualResp.as[String])
        parse(actual) mustBe parse(expected)
      case None =>
        Runtime.default.unsafeRun(actualResp.body.compile.toVector) mustBe empty
    }
  }
}
