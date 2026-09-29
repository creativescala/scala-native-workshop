# The Terminal

In this section we'll learn about the terminal and controlling it using escape codes. We'll also see the limitations of escape codes, which will require us to make system calls to overcome.

## What is the Terminal?

I like to think of the terminal as the web browser of the 1970s. It displayed the funky graphics of the time (colored text), allowed user input (typing), and even supported sound (the single bell sound). One principal way in which it differs is that the browser has become very standardized, whereas terminal standards are much looser. Modern terminals support 24-bit color, mouse input, graphics, and a lot more. Unfortunately you cannot count on any given terminal supporting all of these features. We'll discuss more on terminal standards later.


### Terminals, Shells, and TTYs

When we talk about "the terminal" we often lump together several different things. It's worth pulling them apart, as they will matter later when we start making system calls.

Originally a terminal was a physical device: a keyboard and a screen (or, earlier still, a printer) connected to a computer by a serial line. The DEC VT100, released in 1978, is the most famous example, and many of the escape codes we'll use were popularized there. Even earlier terminals were teletypewriters, which is where the abbreviation *tty* comes from.

Today, nobody has a VT100 on their desk. Instead we run a *terminal emulator*, such as Ghostty, WezTerm, Alacritty, or iTerm2. This is a normal graphical application that pretends to be a physical terminal. It draws text on the screen, interprets escape codes, and turns key presses into bytes.

The serial line has also been replaced by a *pseudo-terminal* (pty) provided by the operating system kernel. A pty is a pair of connected devices. The terminal emulator holds one end, and the programs running "in" the terminal see the other end as their tty device. You can see which tty device you are using by running the `tty` command, which will print something like `/dev/pts/3` on Linux or `/dev/ttys003` on macOS. Sitting in the middle of the pty is the kernel's *tty driver*, which does more than just pass bytes through. It echoes what you type back to the screen, lets you edit a line before sending it to the program, and turns Ctrl-C into a signal that interrupts the running program.

Finally, the *shell*, such as bash, zsh, or fish, is not the terminal at all. It's just a program that reads commands from the tty and writes output to it. When the shell runs one of our programs, that program inherits the same tty as its standard input and output.

Putting this together, we get the following picture.

```
+-------------------+      +------------------------+      +--------------------+
| Terminal emulator | <--> | pty (kernel tty driver)| <--> | Shell / our program|
|  draws text,      |      |  echo, line editing,   |      |  reads stdin,      |
|  interprets       |      |  Ctrl-C -> signal      |      |  writes stdout     |
|  escape codes     |      |                        |      |                    |
+-------------------+      +------------------------+      +--------------------+
```

This picture explains the two different ways we control the terminal. Escape codes are bytes that our program writes. They pass through the tty driver and are interpreted by the terminal emulator, so they control what the emulator does, such as changing colors or moving the cursor. However, some behavior, such as echoing input and waiting for a complete line before our program sees it, lives in the tty driver. Escape codes can't change this, so we'll need system calls to do so.


## Escape Codes

Escape codes are the main way of controlling the terminal. They are an *in-band* form of communication, meaning we print them sprinkled in with text we want the terminal to display. Escape codes are distinguished from normal text by the fact they start with the `ESC` character, as the name suggests. If you've ever seen funky output like `^[[D` in your terminal you have seen escape codes in the wild.

Here are a few definitions to get you started with producing escape codes in Scala.
As you can see, there is a bit of structure in how escape codes are created.

```scala mdoc:silent
/** Escape */
val ESC: Char = '\u001B'

/** The Control Sequence Introducer code, which starts many escape codes. It
  * is ESC[
  */
val csiCode: String = s"${ESC}["

/** Create a CSI escape code. The terminator must be specified first, followed
  * by zero or more arguments. The arguments will be printed semicolon separated
  * before the terminator.
  */
def csi(terminator: String, args: String*): String =
  s"$csiCode${args.mkString(";")}$terminator"

/** Create a Select Graphic Rendition code, which is a form of CSI code that
  * controls graphics effects.
  */
def sgr(n: String): String =
  csi("m", n)
  
// These codes set the foreground color of text
val default: String = sgr("39")
val black: String = sgr("30")
val red: String = sgr("31")
val green: String = sgr("32")
val yellow: String = sgr("33")
val blue: String = sgr("34")
val magenta: String = sgr("35")
val cyan: String = sgr("36")
val white: String = sgr("37")
val brightBlack: String = sgr("90")
val brightRed: String = sgr("91")
val brightGreen: String = sgr("92")
val brightYellow: String = sgr("93")
val brightBlue: String = sgr("94")
val brightMagenta: String = sgr("95")
val brightCyan: String = sgr("96")
val brightWhite: String = sgr("97")
```

Try running the program below. For ease of development you may want to open a Scala console by running `scala console` in your terminal (or run `console` from within sbt) and just paste the code above and below directly into it. You should see some colored output in the terminal.

```scala mdoc:compile-only
print(red)
print("This is red")
print(white)
print("This is white")
print(blue)
print("This is blue")
// Back to normal
print(default)
// Make sure we flush the output so it gets displayed, 
// and create a newline so following text starts on a newline.
println()
```

Not everything that controls the terminal is an escape code. There are also a handful of *control characters*, single bytes that the terminal treats specially. You already know some of them: newline (`\n`) and carriage return (`\r`). Another is the bell character, which is how 1970s terminals made sound. Try it out; on most terminals you'll hear a beep or see the window flash.

```scala mdoc:compile-only
/** The bell character */
val BEL: Char = '\u0007'

print(BEL)
System.out.flush()
```


### Fixing Mistakes

When you start experimenting with escape codes you will make mistakes, and some of these mistakes will leave your terminal in a strange state. Perhaps all your text is now red, the cursor has disappeared, or everything you type comes out as line-drawing characters. The terminal is just doing what it was told. It has no idea that your program has finished and it should go back to normal.

The simplest fix is to send the escape codes that undo the damage. For example, `sgr("0")` (`ESC[0m`) resets all graphics effects, including colors, and you can print it from the shell with

```bash
printf '\e[0m'
```

This requires knowing which escape code will fix the problem, which isn't always obvious. A more general fix is the `tset` command. Just type

```bash
tset
```

and press Enter. `tset` sends the escape codes that initialize the terminal to a sensible state. This undoes most of the mischief we can cause with escape codes.

Later in this workshop we'll change terminal settings using system calls, as well as sending escape codes. If your program crashes before it restores these settings you may find that what you type isn't displayed, or that pressing Enter doesn't do what you expect. For this situation use `reset` instead of `tset`. `reset` is a variant of `tset` that also restores the terminal settings to their defaults. When the terminal is in this state pressing Enter may not work, so type Ctrl-J, then `reset`, then Ctrl-J again. You won't see what you type, but it will work.

If all else fails, you can always close the terminal window and open a new one!


### Lists of Escape Codes

One way that the lack of terminal standardization affects us is that there isn't a single comprehensive listing of all escape codes. Here are some places you can look:

* [Wikipedia](https://en.wikipedia.org/wiki/ANSI_escape_code) lists all of the basic escape codes.
* [XTerm Control Sequences](https://invisible-island.net/xterm/ctlseqs/ctlseqs.html) is a more comprehensive list, with styling that hasn't substantially changed since 1994.
* [Ghostty's Terminal API](https://ghostty.org/docs/vt) is very readable and lists all the codes that Ghostty supports, as well as a conceptual overview.

For our purposes there are two major categories of escape codes, those that

* change the appearance of text (we have already seen some examples of these); and
* allow us to edit what is currently displayed, by deleting text, scrolling text, or moving the cursor around.


@:exercise(Exploring Escape Codes)
 Now it is over to you. Write a simple animation using at least three different escape codes. For example, you could create a spinner or a progress bar. You'll need the following:
 
 * `System.out.flush()` to ensure whatever you print is sent to the terminal
 * `Thread.sleep` to wait between rendering frames. Sleeping for about 100ms should be fine for most animations, but a shorter sleep with give smoother rendering.
 * Escape codes, which you can find in the resources above.
 
Remember to use `tset` or `reset` if you mess up the terminal. For example, you'll probably want to hide the cursor during your animation, but might forget to restore when the animation finishes.
@:@
