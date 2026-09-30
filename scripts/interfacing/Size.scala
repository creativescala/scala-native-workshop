//> using scala 3.9.0
//> using platform native
//> using resourceDir resources

// Exercise 5: get the terminal size portably, getting the value of
// TIOCGWINSZ from C.

import scala.scalanative.posix.sys.ioctl.ioctl
import scala.scalanative.posix.unistd.STDOUT_FILENO
import scala.scalanative.unsafe.*

// struct winsize {
//   unsigned short ws_row;
//   unsigned short ws_col;
//   unsigned short ws_xpixel;
//   unsigned short ws_ypixel;
// };
type winsize =
  CStruct4[CUnsignedShort, CUnsignedShort, CUnsignedShort, CUnsignedShort]

@extern
object WinSize:
  // The C function has a different name, to avoid clashing with other
  // libraries. @name tells Scala Native the name of the C function.
  @name("workshop_tiocgwinsz")
  def TIOCGWINSZ: CLongInt = extern

import WinSize.TIOCGWINSZ

def terminalSize(): (Int, Int) =
  Zone:
    val size = alloc[winsize]()
    if ioctl(STDOUT_FILENO, TIOCGWINSZ, size.asInstanceOf[Ptr[Byte]]) != 0 then
      throw new RuntimeException("Could not get terminal size")
    // Fields are numbered from 1: _1 is ws_row, _2 is ws_col
    (size._1.toInt, size._2.toInt)

@main def size(): Unit =
  val (rows, cols) = terminalSize()
  println(s"$cols columns x $rows rows")
