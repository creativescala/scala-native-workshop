// This is a very simple program that shows some basic formatting in the Terminal
@main def main(): Unit =
  val ESC: Char = '\u001B'
  val default: String = s"$ESC[39m"
  val red: String = s"$ESC[31m"

  println(s"Welcome to ${red}Scala Native!${default}")
