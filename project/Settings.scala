import sbt.License

object Settings {
  val projectName = "Scala Native Workshop"

  // Used to create the artifact names when publishing to Maven.
  val module = "native"
  val organization = "org.creativescala"

  // Used in copyright headers
  val organizationName = "Creative Scala"
  val startYear = Some(2026)

  val license = License.Apache2

  val scalaVersion = "3.9.0"

  // Used to populate the list of the developers. If there are multiple
  // developers edit the values directly in build.sbt
  val githubHandle = "noelwelsh"
  val fullName = "Noel Welsh"

  // Settings for documentation --------------------------------------

  // URL for your Discord or other social site
  val community = "https://discord.gg/rRhcFbJxVG"

  // URL for your source code repository
  val source = "https://github.com/creativescala/scala-native-workshop"

  // URL for Javadoc.
  val javadoc =
    s"https://javadoc.io/doc/${Settings.organization}/${Settings.module}_3/latest/"
}
