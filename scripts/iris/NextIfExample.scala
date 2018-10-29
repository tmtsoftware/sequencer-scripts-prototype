package iris
import tmt.ocs.ScriptImports._
import IrisConstants._

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class NextIfExample(csw: CswServices) extends Script(csw) {
  implicit val ec = strandEc.ec

  handleSetupCommand("doLongRunningComand") { command =>
    spawn {

      val mySetup = Setup(is.prefix, CommandName("longRunningCommnad"), command.maybeObsId)
      val commandFuture = csw.submitAndSubscribe("myAssembly", mySetup)

      var commandComplete = false

      val commandResponse = {

        commandFuture
          .map { response =>
            commandComplete = true
            response
          }
          .recover {
            case NonFatal(exception) =>
              commandComplete = true
              println(s"Command failed: ${exception.getMessage}")
              CommandResponse.Error(command.runId, exception.getMessage)
          }
      }

      loop(1.second) {
        spawn {
          val maybeStopCommand = nextIf(command => command.commandName.name == "stop").await
          if (maybeStopCommand.isDefined) {
            val abortSetup = Setup(is.prefix, CommandName("abortLongRunningCommand"), command.maybeObsId)
            csw.submit("myAssembly", abortSetup).await
          }
          stopWhen(commandComplete)
        }
      }.await

      AggregateResponse(commandResponse.await)
    }
  }
}
