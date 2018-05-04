package tmt.sequencer.scripts.helpers

import tmt.sequencer.ScriptImports.Id
import tmt.sequencer.models.{Command, CommandList}

object CommandListHelper {

  def getSubCommandList(command: Command): CommandList = {
    val subCommandB1 = command.withId(Id(s"${command.id}1"))
    val subCommandB2 = command.withId(Id(s"${command.id}2"))
    CommandList(Seq(subCommandB1, subCommandB2))
  }

  def getSubCommandList(command: Command, name: String): CommandList = {
    val subCommandB1 = command.withName(name).withId(Id(s"${command.id}1"))
    val subCommandB2 = command.withName(name).withId(Id(s"${command.id}2"))
    CommandList(Seq(subCommandB1, subCommandB2))
  }

  def addCommandList(firstList: CommandList, secondList: CommandList): CommandList = {
    firstList.add(secondList)
  }
}
