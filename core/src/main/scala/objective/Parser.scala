package objective

import parsley.Parsley
import parsley.syntax.all.*
import parsley.syntax.character.*

import scala.concurrent.duration.*

object Parser:
  val description = parsley.character.stringOfSome(_ != ':')
  val separator: Parsley[String] = ": "
  val duration: Parsley[Duration] =
    parsley.character.stringOfSome(_.isDigit).map(_.toInt.minutes)

  val status: Parsley[Status] =
    "- [" *> (' '.map(_ => Status.Open) | 'X'.map(_ => Status.Closed)) <* "] "

  val itemLine: Parsley[Item] =
    Item.apply.lift(
      status,
      description <* separator,
      duration
    )

  val otherLine: Parsley[Unit] =
    parsley.combinator
      .manyTill(
        parsley.character.item,
        Parsley.lookAhead(parsley.character.endOfLine | Parsley.eof)
      )
      .void

  val line: Parsley[Option[Item]] =
    itemLine.map(Some.apply).orElse(otherLine.as(None))

  val lines: Parsley[List[Item]] = parsley.combinator
    .sepEndBy(line, parsley.character.endOfLine)
    .map(_.flatten)
