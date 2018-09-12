package iris

import csw.messages.commands.CommandIssue
import csw.messages.params.generics.Parameter
import csw.messages.params.models.{Choice, Units}
import iris.IrisConstants._
import tmt.sequencer.ScriptImports._

class IrisImagerOnly(csw: CswServices) extends Script(csw) {

  csw.handleSetupCommand("setupObservation") { command =>
    spawn {
      val source = command.source
      val commandName = command.commandName
      val obsId = maybeObsId

      val filter = command(is.command.filterKey).head.toString()
      val itime = command(is.command.itimeKey).head
      val ramps = command(is.command.rampsKey).head

      val wheelTargetParameters: Set[Parameter[_]] = sciFilterAssembly.command.wheelKeys.zip(imagerFilterPositions(filter)).map(kv => kv._1.set(kv._2)).toSet
      val filterCommand = Setup(is.prefix, CommandName("select"), command.maybeObsId, wheelTargetParameters)

      val setupImagerCommand = Setup(is.prefix, CommandName("LOAD_CONFIGURATION"), command.maybeObsId)
        .add(imagerDetectorAssembly.command.setupImagerItimeKey.set(itime).withUnits(Units.millisecond))
        .add(imagerDetectorAssembly.command.setupImagerRampKey.set(ramps))

      var response = par(
        csw.submitAndSubscribe(sciFilterAssembly.name, filterCommand),
        csw.submitAndSubscribe(imagerDetectorAssembly.name, setupImagerCommand)
      ).await
      AggregateResponse(response)
    }
  }

  csw.handleSetupCommand("setObserverKeywords") { command =>
    spawn {
      // args to command match event
      csw.publish(SystemEvent(is.prefix, is.event.observerKeywordsEvent, command.paramSet)).await
      AggregateResponse(CommandResponse.Completed(command.runId))
    }
  }

  private val exposureStateEvent = EventKey(imagerDetectorAssembly.prefix, EventName("exposureState"))
  private var currentExposureInProgressEventValue = false

  csw.subscribe(Set(exposureStateEvent)) {
    case ev: SystemEvent =>
      currentExposureInProgressEventValue = ev(imagerDetectorAssembly.event.exposureInProgressKey).head
      Done
    case _ => Done // ignore, shouldn't happen
  }


  private var allThermalStatesMap = collection.mutable.Map[String, Boolean]() ++= cryoenvAssembly.cryoenvStateEventNames.map(e => (e,true)).toMap  // map for storing flag for cold states
  private def okForExposures = allThermalStatesMap.values.forall(identity)

  private val cryoenvStateEvents = cryoenvAssembly.cryoenvStateEventNames.map(name => EventKey(cryoenvAssembly.prefix, EventName(name)))
  csw.subscribe(cryoenvStateEvents) {
    case ev: SystemEvent =>
      val thisThermalState = ev(cryoenvAssembly.event.cryoenvVacuumStateKey).head
      allThermalStatesMap(ev.eventName.name) = thisThermalState == Choice("COLD")

      if (!okForExposures) {
         // if any state is not cold, stop taking exposures and abort
        if (takeImagerExposures) {
          takeImagerExposures = false
          spawn {
            val observeCommand = Observe(is.prefix, CommandName("ABORT_EXPOSURE"), maybeObsId)
            csw.submit(imagerDetectorAssembly.name, observeCommand).await
          }
        }
      }
      Done
    case _ => Done // ignore, shouldn't happen
  }

  private var takeImagerExposures = false
  private var maybeObsId: Option[ObsId] = None
  private var stopExposureLoop = false
  loop {
    spawn {
      if (takeImagerExposures) {
        val observeCommand = Observe(is.prefix, CommandName("START_EXPOSURE"), maybeObsId)
        val response = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
        // check response
      }
      stopWhen(stopExposureLoop) // loop forever
    }
  }


  csw.handleObserveCommand("observe") { command =>
    spawn {
      val commandResponse = command(is.command.imagerObserveKey).head.toString() match {
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
          val observeCommand = Observe(is.prefix, CommandName("ABORT_EXPOSURE"), command.maybeObsId)
          csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
        case x =>
          CommandResponse.Invalid(command.runId, CommandIssue.ParameterValueOutOfRangeIssue("imagerObserve must be START, STOP, or ABORT"))
      }
      AggregateResponse(commandResponse)
    }
  }

  // This is an experimental version of observe using a loop started within
  csw.handleObserveCommand("observe2") { command =>
    spawn {
      val commandResponse = command(is.command.imagerObserveKey).head.toString() match {
        case "START" =>
          if (stopExposureLoop) {
            stopExposureLoop = false
            loop {
              spawn {
                val observeCommand = Observe(is.prefix, CommandName("START_EXPOSURE"), maybeObsId)
                val response = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
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
          val observeCommand = Observe(is.prefix, CommandName("ABORT_EXPOSURE"), command.maybeObsId)
          csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
      }
      AggregateResponse(commandResponse)
    }
  }


  // this doesn't exist in IS command interface, but left here as possible alternative
  csw.handleObserveCommand("singleObserve") { command =>
    spawn {
      val commandName = command(is.command.imagerObserveKey).head.toString() match {
        case "START" => "START_EXPOSURE"
        case "STOP" => "ABORT_EXPOSURE"
        case "ABORT" => "ABORT_EXPOSURE"
      }

      val observeCommand = Observe(is.prefix, CommandName(commandName), command.maybeObsId)
      val response = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
      AggregateResponse(response)
    }
  }

}
