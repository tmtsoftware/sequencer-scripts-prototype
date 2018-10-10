package helpers

import org.scalatest.FunSuite
import ocs.framework.ScriptImports._

class CommandListHelperTest extends FunSuite {

  test("split command into sub-commands") {
    val command        = Setup(Prefix("test-commandB1"), CommandName("setup-iris"), Some(ObsId("test-obsId")))
    val subCommandList = CommandListHelper.getSubCommandList(command)

    val expectedSubCommands = Seq(
      CommandName("setup-iris-subCommand 1"),
      CommandName("setup-iris-subCommand 2")
    )

    assert(subCommandList.commands.map(_.commandName) === expectedSubCommands)
  }
}
