package aoesw

import csw.params.core.generics.KeyType
import csw.params.core.models.Prefix
import ocs.framework.ScriptImports._

class AoeswSync(csw: CswServices) extends Script(csw) {

  object aosq {
    val prefix = Prefix("aoesw.aosq")
    val name = "aoesw-sequencer"
  }

  private val aoeswOffsetTime = KeyType.TAITimeKey.make(name = "scheduledTime")
  private val aoeswOffsetXKey = KeyType.FloatKey.make("x")
  private val aoeswOffsetYKey = KeyType.FloatKey.make("y")

  private val probeOffsetXKey = KeyType.FloatKey.make("x")
  private val probeOffsetYKey = KeyType.FloatKey.make("y")


  handleSetupCommand("offset") { command =>
    spawn {
      val scheduledTime = command(aoeswOffsetTime)
      val offsetX = command(aoeswOffsetXKey)
      val offsetY = command(aoeswOffsetYKey)

      val probeCommand = Setup(aosq.prefix, CommandName("scheduledOffset"), command.maybeObsId)
        .add(probeOffsetXKey.set(offsetX.head))
        .add(probeOffsetYKey.set(offsetY.head))

      csw.scheduler.scheduleOnce(scheduledTime.head) {
        spawn {
          val commandResponse = csw.submit("probeAssembly", probeCommand).await
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
