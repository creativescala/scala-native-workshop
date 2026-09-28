import sbt.*
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*

object Dependencies {
  // Library Versions
  val catsVersion = "2.13.0"
  val catsEffectVersion = "3.7.0-RC1"
  val fs2Version = "3.13.0-M8"
  val parsleyVersion = "5.0.0-M19"
  val declineVersion = "2.6.0"
  val declineDeriveVersion = "0.3.6"

  val scalatagsVersion = "0.13.1"
  val scalajsDomVersion = "2.8.0"

  val scalaCheckVersion = "1.15.4"
  val munitVersion = "1.2.2"
  val munitScalacheckVersion = "1.2.0"
  val munitCatsEffectVersion = "2.1.0"

  // Libraries
  val catsEffect =
    Def.setting("org.typelevel" %%% "cats-effect" % catsEffectVersion)
  val catsCore = Def.setting("org.typelevel" %%% "cats-core" % catsVersion)
  val catsFree = Def.setting("org.typelevel" %%% "cats-free" % catsVersion)
  val fs2 = Def.setting("co.fs2" %%% "fs2-core" % fs2Version)
  val fs2Io = Def.setting("co.fs2" %%% "fs2-io" % fs2Version)
  val parsley = Def.setting("com.github.j-mie6" %%% "parsley" % parsleyVersion)
  val decline = Def.setting("com.monovore" %%% "decline" % declineVersion)
  val declineDerive =
    Def.setting("com.indoorvivants" %%% "decline-derive" % declineDeriveVersion)

  val scalatags = Def.setting("com.lihaoyi" %%% "scalatags" % scalatagsVersion)
  val scalajsDom =
    Def.setting("org.scala-js" %%% "scalajs-dom" % scalajsDomVersion)

  val munit = Def.setting("org.scalameta" %%% "munit" % munitVersion % Test)
  val munitScalaCheck =
    Def.setting(
      "org.scalameta" %%% "munit-scalacheck" % munitScalacheckVersion % Test
    )
  val munitCatsEffect =
    Def.setting(
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectVersion % Test
    )
}
