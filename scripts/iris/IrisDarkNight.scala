package iris

import ocs.framework.ScriptImports._

class IrisDarkNight(csw: CswServices) extends IrisShared(csw) {

  var flag = true

  private val publisherStream = csw.publish(10.seconds) {
    Some(SystemEvent(Prefix("iris-test"), EventName("system")))
  }

  private val subscriptionStream = csw.subscribe(Set(EventKey("iris-test.system"))) { eventKey =>
    println(s"------------------> received-event for iris on key: $eventKey")
    Done
  }

  handleSetupCommand("setup-iris") { command =>
    spawn {
      println(s"[Iris] Received command : ${command.commandName}")
      csw.sendResult(s"[Iris] Received command : ${command.commandName}")

      val command1 = Setup(Prefix("test-commandA1"), CommandName("commandA1"), Some(ObsId("test-obsId")))
      val command2 = Setup(Prefix("test-commandA2"), CommandName("commandA2"), Some(ObsId("test-obsId")))

      csw.addSubCommands(parentCommand = command, childCommands = Set(command1, command2))

      val maybeCommandB = nextIf(c => c.commandName.name == "setup-iris").await
      if (maybeCommandB.isDefined) {
        val commandB = maybeCommandB.get
        csw.sendResult(s"[Iris] Fetched next command: ${commandB.commandName}")
        val commandB1 = Setup(Prefix("test-commandB1"), CommandName("setup-iris"), Some(ObsId("test-obsId")))
        val commandB2 = Setup(Prefix("test-commandB2"), CommandName("setup-iris"), Some(ObsId("test-obsId")))

        csw.addSubCommands(parentCommand = commandB, childCommands = Set(commandB1, commandB2))

        val assemblyResponse3 = csw.submit("Sample1Assembly", commandB1).await
        csw.updateSubCommand(subCmdResponse = assemblyResponse3)

        val assemblyResponse4 = csw.submit("Sample1Assembly", commandB2).await
        csw.updateSubCommand(subCmdResponse = assemblyResponse4)

        csw.sendResult(s"[Iris] Updating response for command: ${commandB.commandName}")
      }

      val assemblyResponse1 = csw.submit("Sample1Assembly", command1).await
      csw.updateSubCommand(subCmdResponse = assemblyResponse1)

      val assemblyResponse2 = csw.submit("Sample1Assembly", command2).await
      csw.updateSubCommand(subCmdResponse = assemblyResponse2)

      csw.sendResult(s"[Iris] Updating response for command: ${command.commandName}")
      Done
    }
  }

  override def onShutdown(): Future[Done] = spawn {
    println("shutdown iris")
    publisherStream.cancel()
    subscriptionStream.unsubscribe().await
    Done
  }

  override def abort(): Future[Done] = spawn {
    flag = true
    Done
  }
}
