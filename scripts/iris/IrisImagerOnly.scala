package iris

import csw.params.commands.CommandIssue
import csw.params.core.models.{Choice, Units}
import iris.IrisConstants._
import ocs.framework.ScriptImports._

class IrisImagerOnly(csw: CswServices) extends Script(csw) {

  handleSetupCommand("setupObservation") { command =>
    spawn {
      // extract info from incoming command
      val filter = command(is.command.filterKey).head.toString()
      val itime  = command(is.command.itimeKey).head
      val ramps  = command(is.command.rampsKey).head

      // get filter target positions for each wheel based on filter in command
      val filterPositions = imagerFilterPositions(filter)
      // construct target parameters
      val wheelTargetParameters = for ((key, filter) <- sciFilterAssembly.command.wheelKeys.zip(filterPositions))
        yield key.set(filter)

      // construct filter command
      val filterCommand = Setup(is.prefix, CommandName("select"), command.maybeObsId, wheelTargetParameters.toSet)

      // construct command to set imager detector settings
      val setupImagerCommand = Setup(is.prefix, CommandName("LOAD_CONFIGURATION"), command.maybeObsId)
        .add(imagerDetectorAssembly.command.setupImagerItimeKey.set(itime).withUnits(Units.millisecond))
        .add(imagerDetectorAssembly.command.setupImagerRampKey.set(ramps))

      // adding sub-commands to CRM
      csw.addSubCommands(parentCommand = command, childCommands = Set(filterCommand, setupImagerCommand))

      // send both commands at same time
      var responses = par(
        csw.submitAndSubscribe(sciFilterAssembly.name, filterCommand),
        csw.submitAndSubscribe(imagerDetectorAssembly.name, setupImagerCommand)
      ).await

      csw.updateSubCommand(responses.head)
      csw.updateSubCommand(responses.last)

      csw.sendResult(s"[Iris] received response: $responses")
      Done
    }
  }

  handleSetupCommand("setObserverKeywords") { command =>
    spawn {
      // args to command match event.  simply reuse and pass on as event.
      csw.publish(SystemEvent(is.prefix, is.event.observerKeywordsEvent, command.paramSet)).await
      csw.addOrUpdateCommand(CommandResponse.Completed(command.runId))
      Done
    }
  }

  // start a subscription to track exposure status (shown as an exampe, not currently used)
  // note this is done in constuctor of Script, so it is always running (not part of a command).
  private val exposureStateEvent                  = EventKey(imagerDetectorAssembly.prefix, EventName("exposureState"))
  private var currentExposureInProgressEventValue = false
  csw.subscribe(Set(exposureStateEvent)) {
    case ev: SystemEvent =>
      currentExposureInProgressEventValue = ev(imagerDetectorAssembly.event.exposureInProgressKey).head
      Done
    case _ => Done // ignore, shouldn't happen since we aren't expecting an ObserveEvent
  }

  // map (EventName -> state) for storing flag for cold states.  Set default state values to true
  private var allThermalStatesMap = collection.mutable
    .Map[String, Boolean]() ++= cryoenvAssembly.cryoenvStateEventNames.map(e => (e, true)).toMap
  // set flag as Boolean And of all values in map.  Note this is defined as a method.
  private def okForExposures = allThermalStatesMap.values.forall(identity)
  // create event keys from list of EventNames
  private val cryoenvStateEvents =
    cryoenvAssembly.cryoenvStateEventNames.map(name => EventKey(cryoenvAssembly.prefix, EventName(name)))
  // subscription to watch
  csw.subscribe(cryoenvStateEvents) {
    case ev: SystemEvent =>
      val thisThermalState = ev(cryoenvAssembly.event.cryoenvVacuumStateKey).head
      allThermalStatesMap(ev.eventName.name) = thisThermalState == Choice("COLD")

      if (!okForExposures) {
        // if any state is not cold, stop taking exposures and abort
        if (takeImagerExposures) {
          takeImagerExposures = false
          spawn {
            // This command is not part of original sequence, we need not to add/update it to CRM
            val observeCommand = Observe(is.prefix, CommandName("ABORT_EXPOSURE"), maybeObsId)
            csw.submit(imagerDetectorAssembly.name, observeCommand).await
          }
        }
      }
      Done
    case _ => Done // ignore, shouldn't happen, since this is a SystemEvent
  }

  // Start a loop in the contructor that is always running
  // Take imager exposures while takeImagerExposures flag is set
  // This flag is set on observe commands in the observe handler below
  private var takeImagerExposures       = false
  private var maybeObsId: Option[ObsId] = None
  private var stopExposureLoop          = false
  loop {
    spawn {
      if (takeImagerExposures) {
        val observeCommand = Observe(is.prefix, CommandName("START_EXPOSURE"), maybeObsId)
        val response       = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
        // check response
      }
      stopWhen(stopExposureLoop) // loop forever.  can be set to true on shutdown.
    }
  }

  handleObserveCommand("observe") { command =>
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
          val response = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
          CommandResponse.withRunId(command.runId, response)
        case x =>
          CommandResponse.Invalid(command.runId,
                                  CommandIssue.ParameterValueOutOfRangeIssue("imagerObserve must be START, STOP, or ABORT"))
      }
      csw.addOrUpdateCommand(commandResponse)
      Done
    }
  }

  // This is an experimental version of observe using a loop started within
  handleObserveCommand("observe2") { command =>
    spawn {
      val commandResponse = command(is.command.imagerObserveKey).head.toString() match {
        case "START" =>
          if (stopExposureLoop) {
            stopExposureLoop = false
            loop {
              spawn {
                val observeCommand = Observe(is.prefix, CommandName("START_EXPOSURE"), maybeObsId)
                val response       = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
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
          val response = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
          CommandResponse.withRunId(command.runId, response)
      }
      csw.addOrUpdateCommand(commandResponse)
      Done
    }
  }

  // this doesn't exist in IS command interface, but left here as possible alternative
  handleObserveCommand("singleObserve") { command =>
    spawn {
      val commandName = command(is.command.imagerObserveKey).head.toString() match {
        case "START" => "START_EXPOSURE"
        case "STOP"  => "ABORT_EXPOSURE"
        case "ABORT" => "ABORT_EXPOSURE"
      }

      val observeCommand = Observe(is.prefix, CommandName(commandName), command.maybeObsId)
      val response       = csw.submitAndSubscribe(imagerDetectorAssembly.name, observeCommand).await
      csw.addSubCommands(command, Set(observeCommand))
      csw.updateSubCommand(response)
      Done
    }
  }

}
