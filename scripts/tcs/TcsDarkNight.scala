package tcs

import ocs.framework.ScriptImports._

class TcsDarkNight(cs: CswServices) extends Script(cs) {

  var eventCount   = 0
  var commandCount = 0

  handleSetupCommand("setup-tcs") { command =>
    spawn {
      println(s"[Tcs] Received command: ${command.commandName}")
      cs.sendResult(s"Command ${command.commandName} received by ${cs.sequencerId}")

      val firstAssemblyResponse = cs.submit("Sample1Assembly", command).await
      val commandFailed         = firstAssemblyResponse.isInstanceOf[CommandResponse.Error]

      val restAssemblyResponses = if (commandFailed) {
        val command2 = Setup(Prefix("test-command2"), CommandName("setup-tcs"), Some(ObsId("test-obsId")))
        Set(cs.submit("Sample1Assembly", command2).await)
      } else {
        val command3 = Setup(Prefix("test-command3"), CommandName("setup-tcs"), Some(ObsId("test-obsId")))
        Set(cs.submit("Sample1Assembly", command3).await)
      }

      val response = AggregateResponse(Completed(command.runId))

      println(s"[Tcs] Received response: $response")
      cs.sendResult(s"[TCS] Received response: $response")
      response
    }
  }
}
