package ocs

import ocs.framework.ScriptImports._

class OcsDarkNight(cs: CswServices) extends Script(cs) {

  val iris = cs.sequenceFeeder("iris")
  val tcs  = cs.sequenceFeeder("tcs")

  var eventCount   = 0
  var commandCount = 0

  val publisherStream = cs.publish(10.seconds) {
    SystemEvent(Prefix("ocs-test"), EventName("system"))
  }

  val subscriptionStream = cs.subscribe(Set(EventKey("ocs-test.system"))) { eventKey =>
    eventCount = eventCount + 1
    println(s"------------------> received-event for ocs on key: $eventKey")
    Done
  }

  handleSetupCommand("setup-iris") { commandA =>
    spawn {
      cs.sendResult(s"Command ${commandA.commandName} received by ${cs.sequencerId}")
      val maybeCommandB = nextIf(c => c.commandName.name == "setup-iris").await
      val subCommandsB = if (maybeCommandB.isDefined) {
        val commandB  = maybeCommandB.get
        val commandB1 = Setup(Prefix("test-commandB1"), CommandName("setup-iris"), Some(ObsId("test-obsId")))
        Sequence.from(commandB, commandB1)
      } else Sequence.empty

      println(s"[Ocs] Received command: ${commandA.commandName}")

      val commandList = subCommandsB.add(commandA)

      iris.await.submit(commandList).await

      val commandAResponse = Completed(commandA.runId)

      val response = if (maybeCommandB.isDefined) {
        AggregateResponse(commandAResponse, Completed(maybeCommandB.get.runId))
      } else {
        AggregateResponse(commandAResponse)
      }

      println(s"[Ocs] Received response: $response")
      cs.sendResult(s"[Ocs] Received response: $response")
      response
    }
  }

  handleSetupCommand("setup-iris-tcs") { commandC =>
    spawn {
      cs.sendResult(s"Command ${commandC.commandName} received by ${cs.sequencerId}")
      val maybeCommandD = nextIf(c2 => c2.commandName.name == "setup-iris-tcs").await
      val tcsSequence = if (maybeCommandD.isDefined) {
        val nextCommand = maybeCommandD.get
        Sequence.from(nextCommand)
      } else {
        Sequence.empty
      }

      println(s"[Ocs] Received command: ${commandC.commandName}")
      val irisSequence = Sequence.from(commandC)

      parAggregate(
        iris.await.submit(irisSequence),
        tcs.await.submit(tcsSequence)
      ).await

      val commandCResponse = Completed(commandC.runId)

      val response = if (maybeCommandD.isDefined) {
        AggregateResponse(commandCResponse, Completed(maybeCommandD.get.runId))
      } else {
        AggregateResponse(commandCResponse)
      }

      println(s"[Ocs] Received response: $response")
      cs.sendResult(s"[Ocs] Received response: $response")
      response
    }
  }

  handleSetupCommand("setup-tcs") { command =>
    spawn {
      cs.sendResult(s"Command ${command.commandName} received by ${cs.sequencerId}")
      println(s"[Ocs] Received command: ${command.commandName}")

      tcs.await.submit(Sequence.from(command)).await

      val response = AggregateResponse(Completed(command.runId))

      println(s"[Ocs] Received response: $response")
      cs.sendResult(s"[Ocs] Received response: $response")
      response
    }
  }

  override def onShutdown(): Future[Done] = spawn {
    subscriptionStream.unsubscribe().await
    publisherStream.cancel()
    println("shutdown ocs")
    Done
  }
}
