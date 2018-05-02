package tmt.sequencer.scripts

import akka.util.Timeout
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.AsyncFunSuite
import org.scalatest.mockito.MockitoSugar
import tmt.sequencer.api.SequenceFeeder
import tmt.sequencer.dsl.{CswServices, FunctionBuilder}
import tmt.sequencer.models.CommandResponse.Success
import tmt.sequencer.models.{AggregateResponse, Command, Id}
import tmt.sequencer.scripts.ocs.OcsDarkNight

import scala.concurrent.Future
import scala.concurrent.duration._

class OcsDarkNightTest extends AsyncFunSuite with MockitoSugar {

  private implicit val timeout: Timeout = Timeout(1.seconds)

  val mockedCsWServices: CswServices                                             = mock[CswServices]
  val mockedSequenceFeeder: SequenceFeeder                                       = mock[SequenceFeeder]
  val commandHandlerBuilder: FunctionBuilder[Command, Future[AggregateResponse]] = new FunctionBuilder

  val inputCommandSequence = List(
    Command(Id("A1"), "setup-iris", List()),
    Command(Id("A2"), "setup-iris", List()),
    Command(Id("B1"), "setup-iris", List()),
    Command(Id("B2"), "setup-iris", List())
  )

  val expectedIrisResponse = AggregateResponse(
    Set(
      Success(Id("A1"), "all children are done"),
      Success(Id("A2"), "all children are done"),
      Success(Id("B1"), "all children are done"),
      Success(Id("B2"), "all children are done")
    )
  )

  val finalOcsResponse: AggregateResponse = expectedIrisResponse.add(
    Success(Id("A"), "all children are done"),
    Success(Id("B"), "all children are done")
  )

  test("OcsDarkNight Script running only setup-iris") {

    when(mockedCsWServices.commandHandlerBuilder).thenReturn(commandHandlerBuilder)
    when(mockedCsWServices.handleCommand(anyString()) { any() }).thenCallRealMethod()
    when(mockedCsWServices.nextIf(any())).thenReturn(
      Future.successful(Some(Command(Id("B"), "setup-iris", List())))
    )
    when(mockedCsWServices.sequenceProcessor("iris")).thenReturn(mockedSequenceFeeder)
    when(mockedSequenceFeeder.feed(inputCommandSequence)).thenReturn(Future.successful(expectedIrisResponse))

    val ocs                               = new OcsDarkNight(mockedCsWServices)
    val result: Future[AggregateResponse] = ocs.execute(Command(Id("A"), "setup-iris", List()))
    result map { res =>
      assert(res === finalOcsResponse)
    }
  }
}
