package tmt.sequencer.scripts.ocs

import tmt.sequencer.ScriptImports._

object OcsFactory {
  def get(cs: CswServices): Script = cs.observingMode match {
    case "darknight"  => new OcsDarkNight(cs)
    case "clearskies" => ???
  }
}
