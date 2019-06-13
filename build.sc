import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository
import mill.define.Sources
import os._

object scripts extends ScalaModule with PublishModule { outer =>
  val rootPath = millSourcePath / up
  def scalaVersion = "2.12.4"
  def publishVersion = "0.0.1"

  def pomSettings = PomSettings(
    description = "These are sequencer scripts",
    organization = "org.tmtsoftware",
    url = "https://github.com/tmtsoftware/sequencer-scripts",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("tmtsoftware", "sequencer-scripts"),
    developers = Seq(
      Developer("mushtaq", "Mushtaq Ahmed","https://github.com/mushtaq"),
    )
  )

  override def repositories = super.repositories ++ Seq(
    MavenRepository("https://jitpack.io")
  )

  override def ivyDeps = Agg(
    ivy"com.github.tmtsoftware.esw-prototype::ocs-framework:b6bdbe4",
    ivy"com.github.tmtsoftware.esw-prototype::ocs-testkit:b6bdbe4"
  )

  override def sonatypeUri: String = "http://localhost:4000/maven/"

  override def mainClass: T[Option[String]] = Some("ocs.framework.SequencerApp")

  override def sources = T.sources(rootPath / "scripts")
  override def resources = T.sources(rootPath / "configs")

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.8"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")

    override def sources: Sources = T.sources(rootPath / "tests" )
  }
}
