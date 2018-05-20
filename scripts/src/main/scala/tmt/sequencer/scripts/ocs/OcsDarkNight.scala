package tmt.sequencer.scripts.ocs

import akka.actor.Cancellable
import akka.stream.KillSwitch
import tmt.sequencer.ScriptImports._
import tmt.sequencer.api.SequenceFeeder
import tmt.sequencer.models.CommandList
import tmt.sequencer.scripts.helpers.CommandListHelper

class OcsDarkNight(cs: CswServices) extends Script(cs) {

  val iris: SequenceFeeder = cs.sequenceProcessor("iris")
  val tcs: SequenceFeeder  = cs.sequenceProcessor("tcs")

  var eventCount   = 0
  var commandCount = 0

  val subscription: KillSwitch = cs.subscribe("ocs") { event =>
    eventCount = eventCount + 1
    println(s"------------------> received-event: ${event.value} on key: ${event.key}")
    Done
  }

  val cancellable: Cancellable = cs.publish(16.seconds) {
    SequencerEvent("ocs-metadata", (eventCount + commandCount).toString)
  }

  cs.handleCommand("setup-iris") { commandA =>
    spawn {
      val maybeCommandB = cs.nextIf(c => c.name == "setup-iris").await
      val subCommandsB = if (maybeCommandB.isDefined) {
        val commandB = maybeCommandB.get
        CommandListHelper.getSubCommandList(commandB)
      } else CommandList.empty

      println(s"[Ocs] Received commandA: ${commandA.name}")

      val subCommandsA = CommandListHelper.getSubCommandList(commandA)
      val commandList  = CommandListHelper.addCommandList(subCommandsA, subCommandsB)

      val response = iris.feed(commandList).await.markSuccessful(commandA).markSuccessful(maybeCommandB)

      println(s"[Ocs] Received response: $response")
      response
    }
  }

  cs.handleCommand("setup-iris-tcs") { commandC =>
    spawn {
      val maybeCommandD = cs.nextIf(c2 => c2.name == "setup-iris-tcs").await
      val tcsSequence = if (maybeCommandD.isDefined) {
        val nextCommand = maybeCommandD.get
        CommandListHelper.getSubCommandList(nextCommand, "setup-tcs")
      } else {
        CommandList.empty
      }

      println(s"[Ocs] Received commandC: ${commandC.name}")
      val irisSequence = CommandListHelper.getSubCommandList(commandC, "setup-iris")
      val aggregateResponse = parAggregate(
        iris.feed(irisSequence),
        tcs.feed(tcsSequence)
      ).await

      val response = aggregateResponse.markSuccessful(commandC).markSuccessful(maybeCommandD)

      println(s"[Ocs] Received response: $response")
      response
    }
  }

  cs.handleCommand("setup-tcs") { command =>
    spawn {
      println(s"[Ocs] Received command: ${command.name}")

      val responseE = tcs.feed(CommandList(Seq(command))).await.markSuccessful(command)

      println(s"[Ocs] Received response: $responseE")
      responseE
    }
  }

  override def onShutdown(): Future[Done] = spawn {
    subscription.shutdown()
    cancellable.cancel()
    println("shutdown")
    Done
  }
}
