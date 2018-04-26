package tmt.sequencer.scripts.tcs

import tmt.sequencer.ScriptImports._

object TcsFactory {
  def get(cs: CswServices): Script = cs.observingMode match {
    case "darknight"  => new TcsDarkNight(cs)
    case "clearskies" => ???
  }
}
