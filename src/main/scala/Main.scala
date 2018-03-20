import tmt.sequencer.ScriptRunner

object Main {
  def main(args: Array[String]): Unit = {
    ScriptRunner.run("scripts/iris_sequencer.sc", isProd = false)
  }
}
