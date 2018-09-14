import tmt.ocs.SequencerApp

object Main {
  def main(args: Array[String]): Unit = {
    val (sequencerId, observingMode, replPort) = args match {
      case Array(sId, oMode, p) => (sId, oMode, p.toInt)
      case _                    => throw new RuntimeException("please provide both sequencerId, observationMode parameters and remote repl port")
    }
    SequencerApp.run(sequencerId, observingMode, replPort)
  }
}
