package iris
import akka.actor.ActorSystem
import csw.params.commands.{CommandName, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.generics.KeyType.IntKey
import csw.params.core.models.{Choice, Prefix}
import iris.IrisConstants.is.filterChoices
import ocs.mocks.{CswServicesMock, SequencerFactory}
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class IrisImagerOnlyTest extends FunSuite {
  test("test handler for command setupObservation") {
    implicit val system: ActorSystem = ActorSystem("test")
    val mockCswServices              = CswServicesMock.create(SequencerFactory.create())
    val irisImagerOnly                = new IrisImagerOnly(mockCswServices)


    /*Without setting values for following keys, it will throw run time exception
    as script does .head to get values script line no - 13 to 15
    * If submitted sequence contains such command then it will throw
    * run time exception and then we need to kill sequencer process as sequencer
    * will be stuck with inflight command
    * Are we looking to catch these kind of errors from tests?? */
    val filterKeyParamset = KeyType.ChoiceKey.make("filter", filterChoices).set(Choice("H"))
    val itimeKeyParamset = IntKey.make("imagerIntegrationTime").set(5)
    val rampsKeyParamset = IntKey.make("imagerNumRamps").set(5)

    val eventualResponse = irisImagerOnly.execute(Setup(Prefix("sequencer"), CommandName("setupObservation"), None, Set(filterKeyParamset, itimeKeyParamset, rampsKeyParamset)))
    println(Await.result(eventualResponse, 10.seconds))

    Await.result(irisImagerOnly.shutdown(), 10.seconds)
    Await.result(system.terminate(), 10.seconds)
  }
}
