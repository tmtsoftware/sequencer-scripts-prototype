package runner

import tmt.sequencer.ScriptRunner

object Main {
  def main(args: Array[String]): Unit = {
    val scriptUnderTest = "scripts/iris_sequencer.sc"
    ScriptRunner.run(scriptUnderTest, isProd = false)
  }
}
