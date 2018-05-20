package tmt.sequencer.scripts.helpers

import org.scalatest.FunSuite
import tmt.sequencer.models.{Command, CommandList, Id}

class CommandListHelperTest extends FunSuite {

  test("split command into sub-commands") {
    val command        = Command(Id("A"), "setup-iris", List(1, 2, 3))
    val subCommandList = CommandListHelper.getSubCommandList(command)

    val expectedCommandList = CommandList(
      List(
        Command(Id("A1"), "setup-iris", List(1, 2, 3)),
        Command(Id("A2"), "setup-iris", List(1, 2, 3))
      )
    )

    assert(subCommandList === expectedCommandList)
  }

  test("split command into sub-commands with specific name") {
    val command        = Command(Id("B"), "setup-iris", List(1, 2, 3))
    val subCommandList = CommandListHelper.getSubCommandList(command, "setup-tcs")

    val expectedCommandList = CommandList(
      List(
        Command(Id("B1"), "setup-tcs", List(1, 2, 3)),
        Command(Id("B2"), "setup-tcs", List(1, 2, 3))
      )
    )

    assert(subCommandList === expectedCommandList)
  }

  test("combine two command lists to create single command list") {
    val firstCommandList = CommandList(
      List(
        Command(Id("A1"), "setup-iris", List(1, 2, 3)),
        Command(Id("A2"), "setup-iris", List(1, 2, 3))
      )
    )

    val secondCommandList = CommandList(
      List(
        Command(Id("B1"), "setup-tcs", List(1, 2, 3)),
        Command(Id("B2"), "setup-tcs", List(1, 2, 3))
      )
    )

    val expectedCommandList = CommandList(
      List(
        Command(Id("A1"), "setup-iris", List(1, 2, 3)),
        Command(Id("A2"), "setup-iris", List(1, 2, 3)),
        Command(Id("B1"), "setup-tcs", List(1, 2, 3)),
        Command(Id("B2"), "setup-tcs", List(1, 2, 3))
      )
    )

    val finalCommandList = CommandListHelper.addCommandList(firstCommandList, secondCommandList)

    assert(finalCommandList === expectedCommandList)
  }

}
