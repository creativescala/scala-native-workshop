package objective

import cats.syntax.all.*
import decline_derive.*

@Name("objective")
enum Command derives CommandApplication:
  def unparse: String =
    this match
      case Serve   => "serve"
      case Inspect => "inspect"
      case Reload  => "reload"
  case Serve
  case Inspect
  case Reload

object Command:
  def parse(string: String): Either[String, Command] =
    string match
      case "serve"   => Command.Serve.asRight
      case "inspect" => Command.Inspect.asRight
      case "reload"  => Command.Reload.asRight
      case other     => other.asLeft

  def unsafeParse(string: String): Command =
    parse(string) match
      case Left(value) =>
        throw new IllegalArgumentException(
          s"Was asked to parse a command $value, which is not a valid command"
        )
      case Right(command) => command
