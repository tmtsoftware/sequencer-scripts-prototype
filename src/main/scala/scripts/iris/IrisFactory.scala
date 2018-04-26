import tmt.sequencer.ScriptImports._

object IrisFactory {
  def get(cs: CswServices): Script = cs.observingMode match {
    case "darknight"  => new IrisDarkNight(cs)
    case "clearskies" => ???
  }
}
