package objective

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Files
import parsley.Result

import scala.concurrent.duration.*

enum Status:
  case Open
  case Closed

final case class Item(status: Status, description: String, duration: Duration)

object Tasks:
  val read: Stream[IO, Result[String, List[Item]]] = Files.forIO
    .readUtf8(Configuration.tasksPath)
    .map(string => Parser.lines.parse(string))
