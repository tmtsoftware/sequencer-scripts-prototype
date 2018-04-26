package tmt.sequencer.scripts.framework

import tmt.sequencer.scripts.iris.IrisFactory
import tmt.sequencer.scripts.ocs.OcsFactory
import tmt.sequencer.scripts.tcs.TcsFactory
import tmt.sequencer.ScriptImports._
import tmt.sequencer.dsl.ScriptFactory

class TopScriptFactory extends ScriptFactory {
  def get(cs: CswServices): Script = cs.sequencerId match {
    case "ocs"  => OcsFactory.get(cs)
    case "iris" => IrisFactory.get(cs)
    case "tcs"  => TcsFactory.get(cs)
  }
}
