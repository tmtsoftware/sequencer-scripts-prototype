package iris

import ocs.framework.ScriptImports._

class IrisDarkNight(cs: CswServices) extends IrisShared(cs) {

  handleObserveCommand("observe-iris") { command =>
    spawn {
      cs.sendResult(s"Command ${command.commandName} received by ${cs.sequencerId}")
      var firstAssemblyResponse: CommandResponse = null
      var counter                                = 0
      loop {
        spawn {
          counter += 1
          cs.sendResult(s"Command ${command.commandName} sending to Sample1Assembly")
          firstAssemblyResponse = cs.submit("Sample1Assembly", command).await
          println(counter)
          stopWhen(counter > 2)
        }
      }.await
      println(s"[Iris] Received command: ${command.commandName}")
      val response = AggregateResponse(Completed(command.runId))

      cs.sendResult(s"[Iris] Received response: $response")
      println(s"[Iris] Received response: $response")
      response
    }
  }

  override def onShutdown(): Future[Done] = spawn {
    println("shutdown iris")
    Done
  }
}
