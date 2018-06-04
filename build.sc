import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository

object scripts extends SbtModule with PublishModule {
  def scalaVersion = "2.12.4"
  def publishVersion = "0.0.1"

  def repositories = super.repositories ++ Seq(
    MavenRepository("https://jitpack.io")
  )

  def ivyDeps = Agg(
    ivy"org.tmt::sequencer-framework:0.1.0-SNAPSHOT"
  )

  object test extends Tests {
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.4"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  def sonatypeUri: String = "http://localhost:4000/maven/"

  def pomSettings = PomSettings(
    description = "These are sequencer scripts",
    organization = "org.tmt",
    url = "https://github.com/tmtsoftware/sequencer-scripts",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("tmtsoftware", "sequencer-scripts"),
    developers = Seq(
      Developer("mushtaq", "Mushtaq Ahmed","https://github.com/mushtaq"),
    )
  )
}