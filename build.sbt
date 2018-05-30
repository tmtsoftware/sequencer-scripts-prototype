
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.tmt",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "sequencer-scripts",
    libraryDependencies += "org.tmt" %% "sequencer-framework" % "0.1.0-SNAPSHOT",
    resolvers += "jitpack" at "https://jitpack.io",
    mappings in Universal ++= {
      val scriptsDirectory = baseDirectory.value / "scripts"
      scriptsDirectory.allPaths pair Path.relativeTo(scriptsDirectory) map {
        case (file, relativePath) => file -> s"scripts/$relativePath"
      }
    }
).enablePlugins(JavaAppPackaging)
