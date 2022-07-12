package hellozio.domain.todo

import hellozio.domain.common.types.StringType
import io.circe.{Codec, CursorOp, Decoder, DecodingFailure, Encoder, Json}
import io.circe.syntax.*

import java.time.Instant

final case class Todo(
    id: Todo.Id,
    task: Todo.Task,
    createdAt: Instant
) derives Codec.AsObject

object Todo {
  opaque type Id = String
  object Id extends StringType[Id]
  opaque type Task = String
  object Task extends StringType[Task]
}

final case class CreateTodo(
    task: Todo.Task,
    createdAt: Instant
)

sealed trait TodoUpdate(val kind: String):
  def id: Todo.Id

object TodoUpdate {
  final case class Created(id: Todo.Id, todo: Todo) extends TodoUpdate("created") derives Codec.AsObject
  final case class Updated(id: Todo.Id, todo: Todo) extends TodoUpdate("updated") derives Codec.AsObject
  final case class Deleted(id: Todo.Id)             extends TodoUpdate("deleted") derives Codec.AsObject

  private val discriminatorField: String = "kind"

  inline given Encoder[TodoUpdate] = Encoder.instance {
    case c: Created => Json.obj(discriminatorField -> Json.fromString(c.kind)).deepMerge(c.asJson)
    case u: Updated => Json.obj(discriminatorField -> Json.fromString(u.kind)).deepMerge(u.asJson)
    case d: Deleted => Json.obj(discriminatorField -> Json.fromString(d.kind)).deepMerge(d.asJson)
  }

  inline given Decoder[TodoUpdate] = Decoder.instance { c =>
    c.downField(discriminatorField).as[String].flatMap {
      case "created" => c.as[Created]
      case "updated" => c.as[Updated]
      case "deleted" => c.as[Deleted]
      case kind      => Left(DecodingFailure(s"Unexpected todo-update kind $kind", List(CursorOp.Field(discriminatorField))))
    }
  }
}
