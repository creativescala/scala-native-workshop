import cats.effect.unsafe.implicits.global
import doodle.core.*
import doodle.core.font.*
import doodle.svg.*
import doodle.syntax.all.*

import scala.scalajs.js.annotation.*

@JSExportTopLevel("Example")
object Example {
  def hello(iteration: Int) = Picture
    .text("Hello!")
    .strokeColor(Color.blueViolet.spin(7.degrees * iteration))
    .strokeWidth(2)
    .fillColor(Color.hotPink.spin(7.degrees * iteration))
    .font(Font.defaultSansSerif.bold.size(FontSize.points(36)))
    .at(iteration * 5, iteration * -3)

  @JSExport
  def draw(id: String): Unit =
    hello(0)
      .on(hello(1))
      .on(hello(2))
      .on(hello(3))
      .on(hello(4))
      .drawWithFrame(Frame(id))
}
