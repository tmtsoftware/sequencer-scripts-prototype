package iris

import csw.messages.commands.CommandIssue
import csw.messages.params.generics.KeyType
import csw.messages.params.models.{Choice, Choices, Units}
import tmt.sequencer.ScriptImports._

class IrisImagerOnly(csw: CswServices) extends Script(csw) {

  private val thisPrefix = Prefix("iris.ici.is")

  private def makeChoices(str: String*): Choices = {
    Choices(str.map(Choice(_)).toSet)
  }

  private val samplingModeChoices = makeChoices("CDS", "MCD", "UTR")
  private val filterChoices = makeChoices("Z", "Y", "J", "H", "K", "Ks", "H+K notch", "CO",
    "BrGamma", "PaBeta", "H2", "FeII", "HeI", "CaII Trip", "J Cont", "H Cont", "K Cont",
    "Zn1", "Zn2", "Zn3", "Zn4", "Yn1", "Yn2", "Yn3", "Yn4", "Jn1", "Jn2", "Jn3", "Jn4", "Jn5",
    "Hn1", "Hn2", "Hn3", "Hn4", "Hn5", "Kn1", "Kn2", "Kn3", "Kn4", "Kn5")

  csw.handleSetupCommand("setupObservation") { command =>
    spawn {
      // parse
      val filterKey = KeyType.ChoiceKey.make("filter", filterChoices)
      val filter = command(filterKey).head.toString()

      val itimeKey = KeyType.IntKey.make("imagerIntegrationTime")
      val itime = command(itimeKey).head

      val rampsKey = KeyType.IntKey.make("imagerNumRamps")
      val ramps = command(rampsKey).head

      val wheels = List("wheel1", "wheel2", "wheel3", "wheel4", "wheel5")

      val filterPositions = filter match {
        case "H" => List("H", "OPEN", "OPEN", "OPEN", "OPEN")
        case "J" => List("J", "OPEN", "OPEN", "OPEN", "OPEN")
        case "K" => List("K", "OPEN", "OPEN", "OPEN", "OPEN")
        case "CO" => List("OPEN", "OPEN", "OPEN", "OPEN", "CO")
        case "Hn1" => List("OPEN", "Hn1", "OPEN", "OPEN", "OPEN")
        case _ => List.empty // invalid (how to handle?)
      }

      val wheelKeys = wheels.map(w => KeyType.StringKey.make(w)) // really an enum key

      val filterCommand = Setup(thisPrefix, CommandName("select"), command.maybeObsId)

      wheelKeys.zip(filterPositions).map(kv => kv._1.set(kv._2)).map(p => filterCommand.add(p))

      val setupImagerItimeKey = KeyType.IntKey.make("rampIntegrationTime")
      val setupImagerRampKey = KeyType.IntKey.make("ramps")
      val setupImagerSamplingModeKey = KeyType.ChoiceKey.make("imagerSamplingMode", samplingModeChoices)

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
      csw.publish(SystemEvent(thisPrefix, EventName("observerKeywords"), command.paramSet)).await
      AggregateResponse(Set(CommandResponse.Completed(command.runId)))
    }
  }

  private val exposureStateEvent = EventKey(Prefix("iris.imager.detector"), EventName("exposureState"))
  private val exposureInProgressKey = KeyType.BooleanKey.make("exposureInProgress")
  private var currentExposureInProgressEventValue = false

  csw.subscribe(Set(exposureStateEvent)) {
    case ev: SystemEvent =>
      currentExposureInProgressEventValue = ev(exposureInProgressKey).head
      Done
    case _ => Done // ignore, shouldn't happen
  }

  private val cryoenvPrefix = Prefix("iris.sc.cryoenv")
  private val cryoenvStateEventNames = Set("IMG_STATE", "IFS_STATE", "WIN_STATE", "PV_STATE", "PRESS_STATE")
  private val cryoenvStateEvents = cryoenvStateEventNames.map(name => EventKey(cryoenvPrefix, EventName(name)))
  private val cryoenvVacuumStates = makeChoices("WARM", "PUMPING", "WARM_VACUUM", "COOLING", "COLD", "WARMING", "PRESSURIZING")
  private val cryoenvVacuumStateKey = KeyType.ChoiceKey.make("thermalVacuumState", cryoenvVacuumStates)


  private var allThermalStatesMap = collection.mutable.Map[String, Boolean]() ++= cryoenvStateEventNames.map(e => (e,true)).toMap  // map for storing flag for cold states
  private def okForExposures = allThermalStatesMap.values.forall(identity)
  csw.subscribe(cryoenvStateEvents) {
    case ev: SystemEvent =>
      val thisThermalState = ev(cryoenvVacuumStateKey).head
      allThermalStatesMap(ev.eventName.name) = thisThermalState == Choice("COLD")

      if (!okForExposures) {
         // if any state is not cold, stop taking exposures and abort
        if (takeImagerExposures) {
          takeImagerExposures = false
          spawn {
            val observeCommand = Observe(thisPrefix, CommandName("ABORT_EXPOSURE"), maybeObsId)
            csw.submit("imager-detector-assembly", observeCommand).await
          }
        }
      }
      Done
    case _ => Done // ignore, shouldn't happen
  }

  private var takeImagerExposures = false
  private var maybeObsId: Option[ObsId] = None
  var stopExposureLoop = false
  loop {
    spawn {
      if (takeImagerExposures) {
        val observeCommand = Observe(thisPrefix, CommandName("START_EXPOSURE"), maybeObsId)
        val response = csw.submitAndSubscribe("imager-detector-assembly", observeCommand).await
        // check response
      }
      stopWhen(stopExposureLoop) // loop forever
    }
  }


  private val imagerObserveChoices = makeChoices("START", "STOP", "ABORT")
  csw.handleObserveCommand("observe") { command =>
    spawn {
      val commandResponse = command(KeyType.ChoiceKey.make("imagerObserve", imagerObserveChoices)).head.toString() match {
        case "START" =>
          if (okForExposures) {
            takeImagerExposures = true
            maybeObsId = command.maybeObsId
            CommandResponse.Completed(command.runId)
          } else {
            CommandResponse.Invalid(command.runId, CommandIssue.WrongInternalStateIssue("Not all IRIS systems are COLD"))
          }
        case "STOP" =>
          takeImagerExposures = false
          CommandResponse.Completed(command.runId)
        case "ABORT" =>
          takeImagerExposures = false
          val observeCommand = Observe(thisPrefix, CommandName("ABORT_EXPOSURE"), command.maybeObsId)
          csw.submitAndSubscribe("imager-detector-assembly", observeCommand).await
        case x =>
          CommandResponse.Invalid(command.runId, CommandIssue.ParameterValueOutOfRangeIssue("imagerObserve must be START, STOP, or ABORT"))
      }
      AggregateResponse(Set(commandResponse))
    }
  }

  // This is an experimental version of observe using a loop started within
  csw.handleObserveCommand("observe2") { command =>
    spawn {
      val commandResponse = command(KeyType.ChoiceKey.make("imagerObserve", imagerObserveChoices)).head.toString() match {
        case "START" =>
          if (stopExposureLoop) {
            stopExposureLoop = false
            loop {
              spawn {
                val observeCommand = Observe(thisPrefix, CommandName("START_EXPOSURE"), maybeObsId)
                val response = csw.submitAndSubscribe("imager-detector-assembly", observeCommand).await
                // check response

                // check for failures
                stopWhen(stopExposureLoop) // loop forever
              }
            }
          }
          CommandResponse.Completed(command.runId)
        case "STOP" =>
          stopExposureLoop = true
          CommandResponse.Completed(command.runId)
        case "ABORT" =>
          stopExposureLoop = true
          val observeCommand = Observe(thisPrefix, CommandName("ABORT_EXPOSURE"), command.maybeObsId)
          csw.submitAndSubscribe("imager-detector-assembly", observeCommand).await
      }
      AggregateResponse(Set(commandResponse))
    }
  }


  // this doesn't exist in IS command interface, but left here as possible alternative
  csw.handleObserveCommand("singleObserve") { command =>
    spawn {
      val commandName = command(KeyType.ChoiceKey.make("imagerObserve", imagerObserveChoices)).head.toString() match {
        case "START" => "START_EXPOSURE"
        case "STOP" => "ABORT_EXPOSURE"
        case "ABORT" => "ABORT_EXPOSURE"
      }

      val observeCommand = Observe(thisPrefix, CommandName(commandName), command.maybeObsId)
      val response = csw.submitAndSubscribe("imager-detector-assembly", observeCommand).await
      AggregateResponse(Set(response))
    }
  }

}
