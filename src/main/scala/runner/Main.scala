package runner

import tmt.sequencer.SequencerApp

object Main {
  def main(args: Array[String]): Unit = {
    SequencerApp.main(Array("iris", "darknight", "8000", "false"))
  }
}
