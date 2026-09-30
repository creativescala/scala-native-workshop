# System Calls

In the previous section we created a program to display an animation, but notably did nothing with terminal input.
This is because usefully handling input requires we interface with C and make system calls.
There are several components to this.
In this section we'll look at calling system functions that Scala Native already provides a binding for.
Along the way we'll meet Scala Native's C types, POSIX bindings, and memory management.
In later sections we'll look at these in more depth.


## Terminal Input

Terminal input uses the same model of escape codes as terminal output.
This time it is special keys, such as the arrows, that the terminal reports using escape codes.
You can see this for yourself. Run `cat` in your terminal, with no arguments, and then enter the famous [Konami code][konami]: up, up, down, down, left, right, left, right, B, A. You should see something like

```
^[[A^[[A^[[B^[[B^[[D^[[C^[[D^[[Cba
```

The terminal displays the `ESC` character as `^[`, so what we are seeing is `ESC[A` (up), `ESC[B` (down), `ESC[C` (right), and `ESC[D` (left), followed by the ordinary characters `b` and `a`. The arrow keys are CSI codes, just like the ones we used for output, with `A` to `D` as the terminator. (Press Ctrl-D, or Ctrl-C, to exit `cat` when you're done.)

Now press Enter. Sadly you don't get 30 extra lives. `cat` receives the input and writes it straight back out, but this time you won't see `^[[A` and friends, just `ba`. `cat` outputs the raw bytes, which the terminal interprets as escape codes that move the cursor. So what was the `^[[A` we saw before we pressed Enter? It wasn't coming from `cat`, which hadn't received any input yet. Hold that thought.


## Cooked and Raw Mode

Remember the tty driver, sitting in the kernel between the terminal emulator and our program? That's where the `^[[A` came from. By default the tty driver is in *cooked* mode (the official name is *canonical* mode). In cooked mode the tty driver does a lot of work on our behalf:

- It *echoes* input, sending what we type back to the terminal so we can see it. Control characters, including `ESC`, are echoed in caret notation like `^[`, which is why we saw `^[[A` for the up arrow.
- It *buffers* input a line at a time. Our program receives nothing until Enter is pressed, at which point it gets the whole line.
- It provides basic *line editing*. Backspace deletes a character, Ctrl-W deletes a word, and Ctrl-U deletes the whole line, all before our program sees any of it.
- It turns some key presses into *signals*. Ctrl-C sends `SIGINT`, which by default terminates our program, and Ctrl-Z sends `SIGTSTP`, which suspends it. Ctrl-D isn't a signal, but it is also handled by the tty driver, which tells our program there is no more input.
- It *translates* some characters. On input, the carriage return sent by the Enter key is turned into a newline. On output, a newline is turned into a carriage return followed by a newline, so the cursor moves back to the start of the next line.

This is great for line-oriented programs like `cat`, which get sensible behavior for free. It is terrible for interactive programs like text editors or games. They want to respond to each key press as soon as it happens, and don't want the tty driver echoing keys or interpreting Ctrl-C on their behalf.

For these programs there is *raw* mode, which turns all of this processing off. In raw mode our program receives each byte as soon as it is typed, nothing is echoed, Ctrl-C is just the byte `3`, and a newline on output moves the cursor down without returning it to the start of the line. This gives us complete control, and also complete responsibility. If we want the user to see what they typed, or to be able to quit with Ctrl-C, we have to implement it ourselves.

You use raw mode more often than you might think. Your shell switches the terminal into (something like) raw mode while you edit a command, which is how it supports the arrow keys, history, and tab completion. It switches back to cooked mode before running the command. That's why the arrow keys work at the shell prompt but not in `cat`.


### Switching Modes

Cooked and raw aren't really two settings. Rather, the tty driver has a large collection of flags, and cooked and raw are two particular combinations of them. For example, the `ECHO` flag controls echoing, `ICANON` controls line buffering and editing, `ISIG` controls whether Ctrl-C and friends generate signals, `ICRNL` controls translating carriage return to newline on input, and `OPOST` controls output processing. Raw mode is, roughly, all of these turned off.

The flags are stored in a C struct called `termios`, and there are two functions we use to work with them:

- `tcgetattr` reads the current settings of a terminal into a `termios` struct; and
- `tcsetattr` changes the settings of a terminal to those in a `termios` struct.

So to switch into raw mode we:

1. call `tcgetattr` to get the current settings, and save a copy of them;
2. turn off the flags for raw mode; and
3. call `tcsetattr` to apply the new settings.

When our program finishes we call `tcsetattr` again with the saved copy to put things back the way we found them. As we saw in the previous section, if we forget to do this, perhaps because our program crashed, the user is left with a terminal that doesn't echo input and needs `reset` to fix.


### Trying It Out

We can play with these settings from the shell using the `stty` command, before writing any code. Run

```bash
stty -a
```

to see all the current settings. Flags that are turned on are listed by name, like `echo` and `icanon`, and flags that are turned off have a `-` in front, like `-echo`.

Now let's turn off line buffering and try `cat` again.

```bash
stty -icanon
cat
```

Type some characters. Every character appears twice: once when the tty driver echoes it, and once when `cat` immediately receives it and writes it back out. Press Ctrl-C to exit `cat`. This still works, as we haven't turned off `isig`. Then restore the normal settings with

```bash
stty sane
```

You might like to try `stty -echo` in the same way, and see how `cat` behaves. (This is how programs read passwords without displaying them.) We don't recommend trying `stty raw` from the shell, as it's not easy to recover from!


## Switching to Raw Mode in Scala Native

Now we know that raw mode is controlled by the `termios` API we can call it from Scala!
Doing so requires learning the details of interfacing with system calls from Scala Native.
This particular case is relatively easy, because Scala Native already provides a [binding][scala-native-posix] to this
API.

The binding lives in `scala.scalanative.posix.termios`. It defines the `termios` struct, the
functions `tcgetattr` and `tcsetattr`, and constants for all the flags. There is also
`scala.scalanative.posix.termiosOps`, which gives us readable names such as `c_lflag` for the
fields of the struct.

Here's the code to switch into raw mode, run some code, and then switch back.

```scala
import scala.scalanative.posix.termios.*
import scala.scalanative.posix.termiosOps.*
import scala.scalanative.posix.unistd.STDIN_FILENO
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object Terminal:
  def withRawMode[A](f: => A): A =
    Zone:
      // Allocate space for two termios structs
      val original = alloc[termios]()
      val raw = alloc[termios]()

      // Get the current settings twice: one copy we keep, one we modify
      if tcgetattr(STDIN_FILENO, original) != 0 ||
        tcgetattr(STDIN_FILENO, raw) != 0
      then throw new RuntimeException("Could not get terminal settings")

      // Turn off the flags for raw mode
      raw.c_iflag =
        raw.c_iflag & ~(BRKINT | ICRNL | INPCK | ISTRIP | IXON).toUInt
      raw.c_oflag = raw.c_oflag & ~OPOST.toUInt
      raw.c_cflag = raw.c_cflag | CS8.toUInt
      raw.c_lflag = raw.c_lflag & ~(ECHO | ICANON | IEXTEN | ISIG).toUInt

      if tcsetattr(STDIN_FILENO, TCSAFLUSH, raw) != 0 then
        throw new RuntimeException("Could not set terminal settings")

      // Run the user's code, and always restore the original settings
      try f
      finally tcsetattr(STDIN_FILENO, TCSAFLUSH, original)
```

There are a few things to note:

- `Zone` creates a [memory zone, or region, or arena allocator][region]. This is
  a portion of memory that is freed when control leaves the scope of the `Zone`.

- `alloc` allocates memory in the zone.

- `tcgetattr` and `tcsetattr` take a *pointer* to a `termios` struct, which is
   what `alloc` gives us. `tcgetattr` fills in the struct we point it to.

- We work on standard input, `STDIN_FILENO`, which is file descriptor 0. This
  is the tty our program inherited from the shell.

- Like most C functions, `tcgetattr` and `tcsetattr` report errors by returning
  a non-zero value, not by throwing an exception. We check for this and throw
  an exception ourselves.

- We use bitwise operations to change the flags. `flags & ~X` turns off `X`,
  and `flags | X` turns it on.

- The flags are unsigned integers. Scala Native extends Scala with these types
  (`UByte`, `UShort`, `UInt`, and `ULong`). We convert standard signed integers
  to unsigned ones using `toUInt`. We do this here because the constants
  `BRKINT` etc. are signed integers. Importing `scala.scalanative.unsigned.*`
  brings in these conversions.

- `TCSAFLUSH` tells `tcsetattr` to apply the change once all pending output has
  been written, and to discard any input that hasn't been read yet.

- The `try`/`finally` makes sure we restore the original settings even if `f`
  throws an exception.

We turn off more flags than we discussed earlier. `IEXTEN` disables Ctrl-V, which on some systems
waits for another character. `IXON` disables Ctrl-S and Ctrl-Q, an ancient form of flow control
that pauses output. The rest (`BRKINT`, `INPCK`, `ISTRIP`, and `CS8`) mostly matter for serial
lines, and are set this way by tradition. This is the same set of flags that the C function
`cfmakeraw` uses.

Now let's use it. The program below prints the byte value of each key you press. Since Ctrl-C no
longer interrupts our program, we quit when the user presses `q`.

```scala
@main def keys(): Unit =
  Terminal.withRawMode:
    var byte = System.in.read()
    while byte != 'q' && byte != -1 do
      // We turned off output processing, so we need \r\n to get a newline
      print(s"$byte\r\n")
      System.out.flush()
      byte = System.in.read()
```

Run it, press some keys, and try the arrow keys. Each arrow key now shows up as three bytes: 27
(`ESC`), 91 (`[`), and then 65 to 68 (`A` to `D`). Try Ctrl-C as well. It's just the byte 3.

@:exercise(Enter the Konami Code)
Write a program that switches into raw mode and waits for the user to enter the Konami code: up,
up, down, down, left, right, left, right, B, A. When they do, reward them with something suitably
over-the-top using the escape codes from the previous section.
@:@

 We've skipped over a lot here: what a `Ptr` really is, how `Zone` manages memory and what happens if we use memory after its zone closes, how C types like `CInt` and `UInt` map to Scala, and how C functions report errors through `errno`. We'll come back to all of these when we write our own bindings.

[konami]: https://en.wikipedia.org/wiki/Konami_Code
[scala-native-posix]: https://scala-native.org/en/latest/lib/posixlib.html
[region]: https://en.wikipedia.org/wiki/Region-based_memory_management
