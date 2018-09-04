package iris

import csw.messages.commands.CommandIssue
import csw.messages.commands.CommandIssue.UnsupportedCommandIssue
import csw.messages.params.generics.KeyType
import csw.messages.params.models.Units
import tmt.sequencer.ScriptImports._

class IrisImagerOnly(csw: CswServices) extends Script(csw) {

  private val thisPrefix = Prefix("iris.ici.is")

  csw.handleCommand("setupObservation") { command =>
    spawn {
      command match {
        case c: Setup => {
          // parse
          val filterKey = KeyType.StringKey.make("filter")
          val filter = c(filterKey).head

          val itimeKey = KeyType.IntKey.make("imagerIntegrationTime")
          val itime = c(itimeKey).head

          val rampsKey = KeyType.IntKey.make("imagerNumRamps")
          val ramps = c(rampsKey).head

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

          val filterCommand = Setup(thisPrefix, CommandName("select"), c.maybeObsId)

          wheelKeys.zip(filterPositions).map(kv => kv._1.set(kv._2)).map(p => filterCommand.add(p))
          // can't add parameters?

          val setupImagerItimeKey = KeyType.IntKey.make("rampIntegrationTime")
          val setupImagerRampKey = KeyType.IntKey.make("ramps")


          val setupImagerCommand = Setup(thisPrefix, CommandName("LOAD_CONFIGURATION"), c.maybeObsId)
            .add(setupImagerItimeKey.set(itime).withUnits(Units.millisecond))
            .add(setupImagerRampKey.set(ramps))

          var response = par(
            csw.submit("sci-filter-assembly", filterCommand),
            csw.submit("imager-detector-assembly", setupImagerCommand)
          ).await
          AggregateResponse(response)
        }
        case x => AggregateResponse(Set(CommandResponse.Invalid(x.runId, UnsupportedCommandIssue("Only Setup commands are supported."))))
      }
    }
  }

  csw.handleCommand("setObserverKeywords") { command =>
    spawn {
      command match {
        case c: Setup => {
          // args to command match event
          csw.publish(SystemEvent(thisPrefix, EventName("observerKeywords")).add(c.paramSet))
          AggregateResponse(Set(CommandResponse.Completed(c.runId)))
        }
        case x => AggregateResponse(Set(CommandResponse.Invalid(x.runId, UnsupportedCommandIssue("Only Setup commands are supported."))))
      }
    }
  }
}
