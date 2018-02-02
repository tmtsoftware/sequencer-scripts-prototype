import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.tmt",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "sequencer-sandbox",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += `sequencer-spike`
  )
