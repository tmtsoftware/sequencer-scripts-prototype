package iris

import ocs.framework.ScriptImports._

class IrisShared(csw: CswServices) extends Script(csw) {

  handleSetupCommand("setup-iris-shared") { command =>
    spawn {
      println(s"[Iris Shared] Received command: ${command.commandName}")
      println("************setup iris from shared script***********")
      csw.sendResult(s"Command ${command.commandName} received by ${csw.sequencerId}")
      var counter = 0
      loop {
        spawn {
          counter += 1
          csw.sendResult(s"Command ${command.commandName} sending to Sample1Assembly")
          csw.submit("Sample1Assembly", command).await
          println(counter)
          stopWhen(counter > 2)
        }
      }.await

      val response = Completed(command.runId)
      csw.addOrUpdateCommand(response)
      csw.sendResult(s"[Iris] Received response: $response")
      println(s"[Iris] Received response: $response")
      Done
    }
  }
}
