package wfos

import csw.params.core.generics.KeyType
import ocs.framework.ScriptImports._
import ocs.framework.dsl.CswServices

class WfosObserve(csw: CswServices) extends Script(csw) {

  private val repeatsKey = KeyType.IntKey.make("repeats")

  private var stopObserving = false

  handleObserveCommand("observe") { command =>
    spawn {
      val repeats = command(repeatsKey)

      var counter = 0
      loop {
        spawn {

          csw.submit("detectorAssembly",
            Setup(Prefix("wfos.sequencer"), CommandName("takeExposure"), command.maybeObsId)).await

          counter += 1
          stopWhen((counter == repeats.head) || stopObserving)
        }
      }
      Done
    }
  }

  override def abort: Future[Done] = {
    spawn {
      stopObserving = true
      csw.submit("detectorAssembly",
        Setup(Prefix("wfos.sequencer"), CommandName("abortExposure"), None))
      Done
    }
  }
}
