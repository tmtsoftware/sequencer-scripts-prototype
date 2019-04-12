package iris

import ocs.framework.ScriptImports._

class IrisAllModes(csw: CswServices) extends Script(csw) {

  handleSetupCommand("setup-iris") { command =>
    spawn {
      // handle setup of Imager and IFU as needed

      Done
    }
  }

  handleObserveCommand("imagerOnly") { command =>
    spawn {
      // handle Observe with Imager Only

      Done
    }
  }

  handleObserveCommand("ifuOnly") { command =>
    spawn {
      // handle Observe with IFU Only

      Done
    }
  }

  handleObserveCommand("imagerAndIfu") { command =>
    spawn {
      // coordinate Imager and IFU observations

      Done
    }
  }


  override def onShutdown(): Future[Done] = spawn {
    println("shutdown iris")
    Done
  }

  override def abort(): Future[Done] = spawn {
    Done
  }
}
