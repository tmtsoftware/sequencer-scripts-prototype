package iris

import ocs.framework.ScriptImports._

class IrisShared(cs: CswServices) extends Script(cs) {

  handleSetupCommand("setup-iris") { command =>
    spawn {
      println("************setup iris from shared script***********")
      cs.sendResult(s"Command ${command.commandName} received by ${cs.sequencerId}")
      var firstAssemblyResponse: CommandResponse = null
      var counter                                = 0
      loop {
        spawn {
          counter += 1
          cs.sendResult(s"Command ${command.commandName} sending to Sample1Assembly")
          firstAssemblyResponse = cs.setup("Sample1Assembly", command).await
          println(counter)
          stopWhen(counter > 2)
        }
      }.await
      println(s"[Iris] Received command: ${command.commandName}")
      val response = AggregateResponse(firstAssemblyResponse)
        .markSuccessful(command)

      cs.sendResult(s"[Iris] Received response: $response")
      println(s"[Iris] Received response: $response")
      response
    }
  }
}
