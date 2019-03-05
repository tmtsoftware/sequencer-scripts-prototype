package tcs

import csw.params.core.generics.KeyType
import csw.params.core.models.Prefix
import ocs.framework.ScriptImports._

class TcsSync(csw: CswServices) extends Script(csw) {

  object tcs {
    val prefix = Prefix("tcs.sequencer")
    val name = "tcs-sequencer"
  }

  private val tcsOffsetTime = KeyType.TAITimeKey.make(name = "scheduledTime")
  private val tcsOffsetXKey = KeyType.FloatKey.make("x")
  private val tcsOffsetYKey = KeyType.FloatKey.make("y")

  private val tpkOffsetXKey = KeyType.FloatKey.make("x")
  private val tpkOffsetYKey = KeyType.FloatKey.make("y")


  handleSetupCommand("offset") { command =>
    spawn {
      val scheduledTime = command(tcsOffsetTime)
      val offsetX = command(tcsOffsetXKey)
      val offsetY = command(tcsOffsetYKey)

      val tcsCommand = Setup(tcs.prefix, CommandName("scheduledOffset"), command.maybeObsId)
        .add(tpkOffsetXKey.set(offsetX.head))
        .add(tpkOffsetYKey.set(offsetY.head))

      csw.scheduler.scheduleOnce(scheduledTime.head) {
        spawn {
          val commandResponse = csw.submit("tpk", tcsCommand).await
          csw.updateSubCommand(commandResponse)
        }
      }

      Done
    }
  }

      override def onShutdown(): Future[Done] = spawn {
    println("shutdown ocs")
    Done
  }
}
