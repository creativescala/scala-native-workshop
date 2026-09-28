package objective

import munit.FunSuite
import parsley.Success

import scala.concurrent.duration.*

class ParserSuite extends FunSuite:
  test("Parser.itemLine parses item"):
    val open = "- [ ] Tickle the cat: 45"
    val closed = "- [X] Tickle the cat : 45"

    assertEquals(
      Parser.itemLine.parse(open),
      Success(Item(Status.Open, "Tickle the cat", 45.minutes))
    )
    assertEquals(
      Parser.itemLine.parse(closed),
      Success(Item(Status.Closed, "Tickle the cat ", 45.minutes))
    )

  test("Parser.otherLine parses any old junk"):
    assertEquals(
      Parser.otherLine.parse("   "),
      Success(())
    )
    assertEquals(
      Parser.otherLine.parse("TITLE: Tasks"),
      Success(())
    )
    assertEquals(Parser.otherLine.parse("\n"), Success(()))
    assertEquals(Parser.otherLine.parse(""), Success(()))

  test("Parser extracts expected items"):
    val input = """
:PROPERTIES:
:ID:       4198917c-52b5-4322-8f5b-883bfdf1ca49
:END:
#+TITLE: Today
- [X] Polish the cat: 45
- [ ] Vacuum the dog: 15
- [ ] Reply to email: 45
    """

    val parsed = Parser.lines.parse(input)

    assertEquals(
      parsed,
      Success(
        List(
          Item(Status.Closed, "Polish the cat", 45.minutes),
          Item(Status.Open, "Vacuum the dog", 15.minutes),
          Item(Status.Open, "Reply to email", 45.minutes)
        )
      )
    )
