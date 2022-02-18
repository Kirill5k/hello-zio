package hellozio.domain.common

import hellozio.domain.todo.Todo
import io.circe.{Decoder, Encoder, Json}
import zio.ZIO
import zio.kafka.serde.{Serde => ZSerde}

object kafka {

  object Serde {
    val todoId: ZSerde[Any, Todo.Id] = ZSerde.string.inmap(Todo.Id.apply)(_.value)

    def json[A](implicit enc: Encoder[A], dec: Decoder[A]): ZSerde[Any, A] =
      ZSerde.string.inmapM(j => ZIO.fromEither(dec.decodeJson(Json.fromString(j))))(v => ZIO(enc(v).noSpaces))
  }

}
