package mocks
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, ControlCommand}
import iris.IrisConstants.{imagerDetectorAssembly, sciFilterAssembly}
import ocs.framework.ScriptImports.toDuration
import ocs.framework.core.SequenceOperator
import ocs.framework.dsl.{CswServices, FutureUtils}
import ocs.testkit.mocks.CswServicesMock

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CswServicesMockWrapper {
  def create(sequencer: SequenceOperator)(implicit system: ActorSystem): CswServices =
    new CswServicesMockWrapper("sequencer1", "mode1", sequencer)
}


class CswServicesMockWrapper(sequencerId: String, observingMode: String, sequencer: SequenceOperator)(implicit system: ActorSystem)
    extends CswServicesMock(sequencerId, observingMode, sequencer) {

  override def submit(
      assemblyName: String,
      command: ControlCommand
  ): Future[SubmitResponse] = {
    assemblyName match {
      case sciFilterAssembly.name =>
        async {
          println(s"submit and subscribe command fired for $assemblyName")
          await(FutureUtils.delay(5.seconds)(Executors.newScheduledThreadPool(2)))
          await(submitResponseF)
        }
      case imagerDetectorAssembly.name =>
        async {
          println(s"submit and subscribe command fired for $assemblyName")
          await(FutureUtils.delay(1.seconds)(Executors.newScheduledThreadPool(2)))
          await(submitResponseF)
        }
      case _ => super.submit(assemblyName, command)
    }
  }

}
