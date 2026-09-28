package objective

import cats.effect.IO
import cats.effect.IOApp
import decline_derive.*
import fs2.Stream

object Main extends CommandApplication.Main[Command]:
  override def run(args: Command) =
    args match
      case Command.Serve   => println("Serve!")
      case Command.Inspect => println("Inspect!")
      case Command.Reload  => println("Reload!")
