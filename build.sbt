
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.tmt",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "sequencer-scripts",
    libraryDependencies += "org.tmt" %% "sequencer-framework" % "0.1.0-SNAPSHOT"
  )
