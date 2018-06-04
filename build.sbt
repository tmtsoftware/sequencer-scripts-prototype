
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
  )
