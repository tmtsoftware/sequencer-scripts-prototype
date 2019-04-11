package examples

import ocs.framework.ScriptImports._

class DemoSequencer(csw: CswServices) extends Script(csw) {

  private var eventCount = 0

  private val publisherStream = csw.publish(5.seconds) {
    Some(SystemEvent(Prefix("demo-test"), EventName("system")))
  }

  private val subscriptionStream = csw.subscribe(Set(EventKey("demo-test.system"))) { eventKey =>
    eventCount = eventCount + 1
    println(s"------------------> received-event for demo sequencer on key: $eventKey")
    Done
  }

  handleSetupCommand("setup") { command =>
    var loopCount = 0
    println(s"[Demo] Received command: ${command.commandName}")

    spawn {
      loop(500.millis) {
        spawn {
          loopCount = loopCount + 1
          println(s"Handling long running command, subscriptionStream got $eventCount events")
          stopWhen(loopCount > 50)
        }
      }.await

      val response = csw.submit("Sample1Assembly", command).await
      csw.addOrUpdateCommand(response)
      Done
    }
  }

  handleObserveCommand("observe") { command =>
    spawn {
      println(s"[Demo] Received command: ${command.commandName}")
      val response = csw.submit("Sample1Assembly", command).await
      csw.addOrUpdateCommand(response)
      Done
    }
  }

  override def onShutdown(): Future[Done] = spawn {
    println("shutdown ocs")
    subscriptionStream.unsubscribe().await
    publisherStream.cancel()
    Done
  }
}
