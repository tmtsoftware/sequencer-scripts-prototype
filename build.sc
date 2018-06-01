import mill._
import mill.scalalib._
import coursier.maven.MavenRepository

object runner extends SbtModule {
  def scalaVersion = "2.12.4"

  def repositories = super.repositories ++ Seq(
    MavenRepository("https://jitpack.io")
  )

  def ivyDeps = Agg(
    ivy"org.tmt::sequencer-framework:0.1.0-SNAPSHOT"
  )

}
