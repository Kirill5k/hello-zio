package hellozio.domain.common

import fs2.kafka.{Deserializer, Serializer}
import hellozio.domain.todo.Todo
import io.circe.jawn._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import zio.interop.catz._
import zio.{Clock, RIO}

import java.nio.charset.StandardCharsets

object kafka {

  object Serde {
    implicit def todoIdSerializer: Serializer[RIO[Clock, *], Todo.Id] =
      Serializer.instance[RIO[Clock, *], Todo.Id] { case (_, _, id) => RIO.succeed(id.value.getBytes(StandardCharsets.UTF_8)) }

    implicit def todoIdDeserializer: Deserializer[RIO[Clock, *], Todo.Id] =
      Deserializer.instance[RIO[Clock, *], Todo.Id] { case (_, _, b) => RIO.succeed(Todo.Id(new String(b, StandardCharsets.UTF_8))) }

    implicit def jsonSerializer[A: Encoder]: Serializer[RIO[Clock, *], A] =
      Serializer.instance[RIO[Clock, *], A] { case (_, _, a) => RIO(a.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)) }

    implicit def jsonDeserializer[A: Decoder]: Deserializer[RIO[Clock, *], A] =
      Deserializer.instance[RIO[Clock, *], A] { case (_, _, a) => RIO.fromEither(decodeByteArray[A](a)) }
  }

}
