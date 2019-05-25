package iris
import iris.IrisConstants.is
import ocs.framework.ScriptImports._

import scala.util.control.NonFatal

class StopExample(csw: CswServices) extends Script(csw) {
  implicit val ec = strandEc.ec

  var sendStopCommand = false
  handleSetupCommand("doLongRunningComand") { command =>
    spawn {

      sendStopCommand = false
      val mySetup = Setup(is.prefix, CommandName("longRunningCommnad"), command.maybeObsId)
      val commandFuture = csw.submit("myAssembly", mySetup)

      var commandComplete = false
      val commandResponse = {
        commandFuture.map { response =>
            commandComplete = true
            response
          }.recover {
            case NonFatal(exception) =>
              commandComplete = true
              println(s"Command failed: ${exception.getMessage}")
              CommandResponse.Error(command.runId, exception.getMessage)
          }
      }
      loop(1.second) {
        spawn {
          if (sendStopCommand) {
            val abortSetup = Setup(is.prefix, CommandName("abortLongRunningCommand"), command.maybeObsId)
            csw.submit("myAssembly", abortSetup).await
            sendStopCommand = false
          }
          stopWhen(commandComplete)
        }
      }.await

      commandResponse.await
      Done
    }
  }
  override def abort(): Future[Done] = {
    spawn {
      sendStopCommand = true
      Done
    }
  }
}