//> using scala 3.9.0
//> using platform native

import scala.scalanative.unsafe.*

@extern
object Unistd:
  def isatty(fd: CInt): CInt = extern

@main def isATty(): Unit =
  println(s"stdin: ${Unistd.isatty(0)}, stdout: ${Unistd.isatty(1)}")
