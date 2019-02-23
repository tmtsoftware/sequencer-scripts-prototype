package ocs

import csw.params.core.models.Prefix
import ocs.framework.ScriptImports._

class LGSAcquisition (csw: CswServices) extends Script(csw){

  object ocs {
    val prefix = Prefix("esw.ocs")
    val name = "esw-ocs-sequencer"
  }

  private val tcs  = csw.sequencerCommandService("tcs")
  private val aosq  = csw.sequencerCommandService("aoesw")
  private val irissq  = csw.sequencerCommandService("iris")

  // 3.1.1 in Workflows
  handleSetupCommand("preset") { command =>
    spawn {
      val response = par {
        aosq.await.submit(Sequence(Setup(ocs.prefix, CommandName("Slew AOSQ"), command.maybeObsId)))
        tcs.await.submit(Sequence(Setup(ocs.prefix, CommandName("Slew TCS"), command.maybeObsId)))
        irissq.await.submit(Sequence(Setup(ocs.prefix, CommandName("Slew IRIS"), command.maybeObsId)))
      }.await

      // validate response?
      Done
    }
  }

  // 3.1.2 in Workflows
  handleSetupCommand("acquire") { command =>
    spawn {

      // configure OIWFS probes
      // enable Ttf in AOSQ

      Done
    }
  }
}
