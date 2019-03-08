package ocs

import csw.params.core.generics.KeyType
import csw.params.core.models.Prefix
import ocs.framework.ScriptImports._

class OcsSync(csw: CswServices) extends Script(csw) {

  object ocs {
    val prefix = Prefix("esw.ocs")
    val name   = "esw-ocs-sequencer"
  }

  private val aosq = csw.sequencerCommandService("aoesw")
  private val tcs  = csw.sequencerCommandService("tcs")

  private val offsetTime = KeyType.TAITimeKey.make(name = "scheduledTime")
  private val offsetXKey = KeyType.FloatKey.make("x")
  private val offsetYKey = KeyType.FloatKey.make("y")

  private val aoOffsetTime = KeyType.TAITimeKey.make(name = "scheduledTime")
  private val aoOffsetXKey = KeyType.FloatKey.make("x")
  private val aoOffsetYKey = KeyType.FloatKey.make("y")

  private val tcsOffsetTime = KeyType.TAITimeKey.make(name = "scheduledTime")
  private val tcsOffsetXKey = KeyType.FloatKey.make("x")
  private val tcsOffsetYKey = KeyType.FloatKey.make("y")

  private def transformOffsetToAo(x: Float, y: Float) = {
    // some transformation
    (x, y)
  }

  handleSetupCommand("offset") { command =>
    spawn {
      val scheduledTime = command(offsetTime)
      val offsetX       = command(offsetXKey)
      val offsetY       = command(offsetYKey)

      val aoOffset = transformOffsetToAo(offsetX.head, offsetY.head)

      val aoCommand = Setup(ocs.prefix, CommandName("scheduledOffset"), command.maybeObsId)
        .add(aoOffsetXKey.set(aoOffset._1))
        .add(aoOffsetYKey.set(aoOffset._2))
        .add(aoOffsetTime.set(scheduledTime.head))

      val tcsCommand = Setup(ocs.prefix, CommandName("scheduledOffset"), command.maybeObsId)
        .add(tcsOffsetXKey.set(offsetX.head))
        .add(tcsOffsetYKey.set(offsetY.head))
        .add(tcsOffsetTime.set(scheduledTime.head))

      csw.addSubCommands(command, Set(aoCommand, tcsCommand))

      var responses = par {
        tcs.await.submit(Sequence(tcsCommand))
        aosq.await.submit(Sequence(aoCommand))
      }.await

      csw.updateSubCommand(CommandResponse.withRunId(tcsCommand.runId,responses.head))
      csw.updateSubCommand(CommandResponse.withRunId(aoCommand.runId,responses.last))

      Done
    }
  }

  override def onShutdown(): Future[Done] = spawn {
    println("shutdown ocs")
    Done
  }
}
