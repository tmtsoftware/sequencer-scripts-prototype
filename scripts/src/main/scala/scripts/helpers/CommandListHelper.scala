package scripts.helpers

import tmt.sequencer.ScriptImports._

object CommandListHelper {

  def getSubCommandList(command: SequenceCommand): CommandList = {
    val subCommandB1 =
      Setup(Prefix("test-sub-commandB1"), CommandName(s"${command.commandName.name}-subCommand 1"), Some(ObsId("test-obsId")))
    val subCommandB2 =
      Setup(Prefix("test-sub-commandB1"), CommandName(s"${command.commandName.name}-subCommand 2"), Some(ObsId("test-obsId")))
    CommandList(Seq(subCommandB1, subCommandB2))
  }
}
