package iris

import csw.messages.commands.CommandIssue
import csw.messages.params.generics.KeyType
import csw.messages.params.models.Units
import tmt.sequencer.ScriptImports._

class IrisImagerOnly(csw: CswServices) extends Script(csw) {

  private val thisPrefix = Prefix("iris.ici.is")


  csw.handleSetupCommand("setupObservation") { command =>
    spawn {
      // parse
      val filterKey = KeyType.StringKey.make("filter")
      val filter = command(filterKey).head

      val itimeKey = KeyType.IntKey.make("imagerIntegrationTime")
      val itime = command(itimeKey).head

      val rampsKey = KeyType.IntKey.make("imagerNumRamps")
      val ramps = command(rampsKey).head

      val wheels = List("wheel1", "wheel2", "wheel3", "wheel4", "wheel5")

      val filterPositions = filter match {
        case "H" => List("H", "OPEN", "OPEN", "OPEN", "OPEN")
        case "J" => List("J", "OPEN", "OPEN", "OPEN", "OPEN")
        case "K" => List("K", "OPEN", "OPEN", "OPEN", "OPEN")
        case "Dark" => List("OPEN", "OPEN", "OPEN", "OPEN", "DARK")
        case "H-Alpha" => List("OPEN", "HAlpha", "OPEN", "OPEN", "OPEN")
        case _ => List.empty // invalid (how to handle?)
      }

      val wheelKeys = wheels.map(w => KeyType.StringKey.make(w)) // really an enum key

      val filterCommand = Setup(thisPrefix, CommandName("select"), command.maybeObsId)

      wheelKeys.zip(filterPositions).map(kv => kv._1.set(kv._2)).map(p => filterCommand.add(p))

      val setupImagerItimeKey = KeyType.IntKey.make("rampIntegrationTime")
      val setupImagerRampKey = KeyType.IntKey.make("ramps")

      val setupImagerCommand = Setup(thisPrefix, CommandName("LOAD_CONFIGURATION"), command.maybeObsId)
        .add(setupImagerItimeKey.set(itime).withUnits(Units.millisecond))
        .add(setupImagerRampKey.set(ramps))

      var response = par(
        csw.submitAndSubscribe("sci-filter-assembly", filterCommand),
        csw.submitAndSubscribe("imager-detector-assembly", setupImagerCommand)
      ).await
      AggregateResponse(response)
    }
  }

  csw.handleSetupCommand("setObserverKeywords") { command =>
    spawn {
      // args to command match event
      csw.publish(SystemEvent(thisPrefix, EventName("observerKeywords"), command.paramSet))
      AggregateResponse(Set(CommandResponse.Completed(command.runId)))
    }
  }

  private val exposureStateEvent = EventKey(Prefix("iris.imager.detector"), EventName("exposureState"))
  private val exposureInProgressKey = KeyType.BooleanKey.make("exposureInProgress")
  private var currentExposureInProgressEventValue = false

  csw.subscribe(Set(exposureStateEvent)) { event =>
    event match {
      case ev: SystemEvent =>
        currentExposureInProgressEventValue = ev(exposureInProgressKey).head
    }
    Done
  }

  private var takeImagerExposures = false
  private var maybeObsId: Option[ObsId] = None
  loop {
    spawn {
      if (takeImagerExposures) {
        val observeCommand = Observe(thisPrefix, CommandName("START_EXPOSURE"), maybeObsId)
        val response = csw.submitAndSubscribe("imager-detector-assembly", observeCommand).await
        // check response

        // check for failures
      }
      stopWhen(false) // loop forever
    }
  }

  csw.handleObserveCommand("observe") { command =>
    spawn {
      val commandResponse = command(KeyType.StringKey.make("imagerObserve")).head match {
        case "START" =>
          takeImagerExposures = true
          maybeObsId = command.maybeObsId
          CommandResponse.Completed(command.runId)
        case "STOP" =>
          takeImagerExposures = false
          CommandResponse.Completed(command.runId)
        case "ABORT" =>
          takeImagerExposures = false
          val observeCommand = Observe(thisPrefix, CommandName("ABORT_EXPOSURE"), command.maybeObsId)
          csw.submit("imager-detector-assembly", observeCommand).await
        case x =>
          CommandResponse.Invalid(command.runId, CommandIssue.ParameterValueOutOfRangeIssue("imagerObserve must be START, STOP, or ABORT"))
      }
      AggregateResponse(Set(commandResponse))
    }
  }


  // this doesn't exist in IS command interface, but left here as possible alternative
  csw.handleObserveCommand("singleObserve") { command =>
    spawn {
      val commandName = command(KeyType.StringKey.make("imagerObserve")).head match {
        case "START" => "START_EXPOSURE"
        case "STOP" => "ABORT_EXPOSURE"
        case "ABORT" => "ABORT_EXPOSURE"
      }

      val observeCommand = Observe(thisPrefix, CommandName(commandName), command.maybeObsId)
      val response = csw.submit("imager-detector-assembly", observeCommand).await
      AggregateResponse(Set(response))
    }
  }

}
