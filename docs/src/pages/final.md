# Bringing it Together

At this point we know a lot about using Scala Native and terminal programming.
It's time to bring it all together and implement something substantial.


## What We Can Do

Let's recap what we've built so far.
We can:

- control colors and move the cursor with escape codes;
- switch the terminal into raw mode, and restore it when we're done;
- read individual key presses, including arrow keys and the Escape key;
- run a game loop, using reads with a timeout;
- find the size of the terminal; and
- react when the terminal is resized.

Before starting your project, you might find it useful to collect the code from the exercises into a small `Terminal` module.
This gives you a toolkit to build on, rather than copying code from exercise solutions.


## Choosing a Project

It is up to you what you create.
Choose whatever gets you excited, but here are some ideas, roughly from smallest to largest:

- **Conway's Game of Life.** Mostly drawing, with a game loop to step the simulation and a few keys to pause and quit.
- **A todo list.** Something both simple and useful. Arrow keys move between items, and other keys add, complete, and delete them.
- **A pager**, like `less`. Display a file one screen at a time, using the terminal size to know how much fits, and the arrow keys to scroll.
- **Snake.** This uses almost everything from the workshop: a game loop driven by reads with a timeout, arrow keys to steer, and the terminal size to set the size of the board.


## Tips

Here are a few things that will make your life easier.

- **Use the alternate screen.** Terminals have a second screen buffer, used by full-screen programs like `vim` and `less`. Switch to it with `ESC[?1049h` when your program starts, and back with `ESC[?1049l` when it finishes. The user's shell session and scrollback are left untouched, and reappear when your program exits.
- **Hide the cursor** while your program is running, using `ESC[?25l`, and show it again using `ESC[?25h`. Do this in the same `finally` block that restores the terminal settings, so the cursor comes back even if your program crashes.
- **Draw a whole frame at once.** Build up the output for each frame in a `StringBuilder`, and write it with a single `print` followed by a `flush`. Writing a little at a time can cause visible flicker.
- **Log to a file.** Anything you `println` for debugging gets mixed into your user interface. Instead write debugging output to a file, and watch it with `tail -f` in another terminal.


## Resources

Here are a few resources that might help you:

- [Cats Effect][cats-effect] runs on Scala Native! Since version 3.7 it supports multiple threads on Scala Native. If you want to set up multiple threads (for example, one thread for reading input and another for rendering) it will make your life easier. [fs2][fs2], which we mentioned when setting up the project, is built on Cats Effect and is useful for working with streams of input.

- [Terminus][terminus] is a terminal library that runs on the JVM, Native, and JS platforms. It includes low-level functions like the ones we have worked on, as well as a higher-level terminal user interface (TUI) toolkit. (The TUI toolkit is unfortunately not documented at the time of writing. There are several [examples], though.)

- [sn-bindgen][sn-bindgen] generates Scala Native bindings from C header files. Writing bindings by hand, as we have done, is a great way to learn how they work, but for a large C library it quickly becomes tedious. sn-bindgen does the work for you.


## Wrapping Up

In this workshop we've gone from printing escape codes to making system calls, writing our own bindings, and mixing C code into a Scala program.
Along the way we've seen how the terminal really works, and how Scala Native lets us reach below the abstractions the JVM provides.
We hope you've enjoyed it, and we'd love to see what you build!

[cats-effect]: https://typelevel.org/cats-effect/
[fs2]: https://fs2.io/
[terminus]: https://www.creativescala.org/terminus/
[examples]: https://github.com/creativescala/terminus/blob/main/ui/native/src/main/scala/terminus/ui/Example.scala
[sn-bindgen]: https://sn-bindgen.indoorvivants.com/
