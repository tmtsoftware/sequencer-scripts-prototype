import tmt.sequencer.ScriptImports._
import $file.iris_sequencer
import $file.tcs_sequencer
import $file.helpers
import scala.collection.mutable

init[OcsSequencer]

class OcsSequencer(cs: CommandService) extends Script(cs) {

  var results: mutable.Buffer[CommandResult] = mutable.Buffer.empty
  val iris = new iris_sequencer.IrisSequencer(cs)
  val tcs = new tcs_sequencer.TcsSequencer(cs)

  override def onSetup(command: Command): CommandResult = {
    if (command.name == "setup-iris") {
      println("*" * 150)
      println(s"\nCommand received: [Ocs-sequencer] - ${command.name}")
      val result = iris.onSetup(command)
      results += result
      Thread.sleep(10000)
      println(s"\nResult received: [Ocs-sequencer] - ${result}")
      println("*" * 150 + "\n\n")
      result
    } else if (command.name == "setup-tcs") {
      println("*" * 150)
      println(s"\nCommand received: [Ocs-sequencer] - ${command.name}")
      val result = tcs.onSetup(command)
      results += result
      Thread.sleep(10000)
      println(s"\nResult received: [Ocs-sequencer] - ${result}")
      println("*" * 150 + "\n\n")
      result
    }
    else {
      println(s"unknown command=$command")
      CommandResult.Empty
    }
  }


  override def onObserve(x: Command): CommandResult = {
    println("observe")
    val x = helpers.square(99)
    println(x)
    CommandResult.Single(x.toString)
  }

  override def onShutdown(): Unit = {
    println("shutdown")
  }

  override def onEvent(event: SequencerEvent): Unit = {
    println(event)
  }
}
