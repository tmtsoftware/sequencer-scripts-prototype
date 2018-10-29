package iris

import iris.IrisConstants.is
import tmt.ocs.ScriptImports._

import scala.util.control.NonFatal

class StopExample2(csw: CswServices) extends Script(csw) {
  implicit val ec = strandEc.ec

  var inLongRunningCommand = false
  handleSetupCommand("doLongRunningComand") { command =>
    spawn {

      inLongRunningCommand = true
      val mySetup = Setup(is.prefix, CommandName("longRunningCommnad"), command.maybeObsId)
      val commandResponse = csw.submitAndSubscribe("myAssembly", mySetup).await
      inLongRunningCommand = false

      AggregateResponse(commandResponse)

    }.recover {
      case NonFatal(exception) =>
        println(s"Command failed: ${exception.getMessage}")
        AggregateResponse(CommandResponse.Error(command.runId, exception.getMessage))
    }
  }
  override def onStop(): Future[Done] = {
    spawn {
      if (inLongRunningCommand) {
        val abortSetup = Setup(is.prefix, CommandName("abortLongRunningCommand"), None)
        csw.submit("myAssembly", abortSetup).await
        // Handle error
      }
      Done
    }
  }
}