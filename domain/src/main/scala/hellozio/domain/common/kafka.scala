package hellozio.domain.common

import fs2.kafka.{Deserializer, Serializer}
import hellozio.domain.todo.Todo
import io.circe.{Decoder, Encoder, Json}
import io.circe.jawn._
import io.circe.syntax._
import zio.{Clock, RIO, ZIO}
import zio.kafka.serde.{Serde => ZSerde}
import zio.interop.catz._

import java.nio.charset.StandardCharsets

object kafka {

  object Serde {
    val todoId: ZSerde[Any, Todo.Id] = ZSerde.string.inmap(Todo.Id.apply)(_.value)

    implicit def todoIdSerializer: Serializer[RIO[Clock, *], Todo.Id] =
      Serializer.instance[RIO[Clock, *], Todo.Id] { case (_, _, id) => RIO.succeed(id.value.getBytes(StandardCharsets.UTF_8)) }

    implicit def todoIdDeserializer: Deserializer[RIO[Clock, *], Todo.Id] =
      Deserializer.instance[RIO[Clock, *], Todo.Id] { case (_, _, b) => RIO.succeed(Todo.Id(new String(b, StandardCharsets.UTF_8))) }

    implicit def jsonSerializer[A: Encoder]: Serializer[RIO[Clock, *], A] =
      Serializer.instance[RIO[Clock, *], A] { case (_, _, a) => RIO(a.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)) }

    implicit def jsonDeserializer[A: Decoder]: Deserializer[RIO[Clock, *], A] =
      Deserializer.instance[RIO[Clock, *], A] { case (_, _, a) => RIO.fromEither(decodeByteArray[A](a)) }

    def json[A](implicit enc: Encoder[A], dec: Decoder[A]): ZSerde[Any, A] =
      ZSerde.string.inmapM(j => ZIO.fromEither(dec.decodeJson(Json.fromString(j))))(v => ZIO(enc(v).noSpaces))
  }

}
