package tcs

import csw.params.core.generics.KeyType
import csw.params.core.models.Prefix
import ocs.framework.ScriptImports._

class TcsSync(csw: CswServices) extends Script(csw) {

  object tcs {
    val prefix = Prefix("tcs.sequencer")
    val name   = "tcs-sequencer"
  }

  private val tcsOffsetTime = KeyType.TAITimeKey.make(name = "scheduledTime")
  private val tcsOffsetXKey = KeyType.FloatKey.make("x")
  private val tcsOffsetYKey = KeyType.FloatKey.make("y")

  private val tpkOffsetXKey = KeyType.FloatKey.make("x")
  private val tpkOffsetYKey = KeyType.FloatKey.make("y")

  handleSetupCommand("offset") { command =>
    spawn {
      val scheduledTime = command(tcsOffsetTime)
      val offsetX       = command(tcsOffsetXKey)
      val offsetY       = command(tcsOffsetYKey)

      val tcsCommand = Setup(tcs.prefix, CommandName("scheduledOffset"), command.maybeObsId)
        .add(tpkOffsetXKey.set(offsetX.head))
        .add(tpkOffsetYKey.set(offsetY.head))

      /*
       * as the response would have command ID of tcsCommand(line 27)
       * which is a different command then the original command received.
       * you can add tcsCommand as subcommand to original command and then
       * give the response directly to CRM, since it will first mark the subcommand complete and
       * parent command would be inferred automatically
       *
       * csw.addSubCommands(command, Set(tcsCommand))
       * csw.updateSubCommand(commandResponse)
      */


      csw.addSubCommands(command, Set(tcsCommand))

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
