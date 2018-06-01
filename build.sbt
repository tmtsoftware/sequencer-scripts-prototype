
lazy val runner = project
  .enablePlugins(JavaAppPackaging)
  .settings(
    inThisBuild(List(
      organization := "org.tmt",
      scalaVersion := "2.12.4",
      version := "0.1.0-SNAPSHOT"
    )),
    libraryDependencies += Libs.`sequencer-framework`,
    resolvers += "jitpack" at "https://jitpack.io",
    mappings in Universal ++= {
      val scriptsDirectory = baseDirectory.value / "scripts"
      scriptsDirectory.allPaths pair Path.relativeTo(scriptsDirectory) map {
        case (file, relativePath) => file -> s"scripts/$relativePath"
      }
    }
  ).enablePlugins(JavaAppPackaging)
