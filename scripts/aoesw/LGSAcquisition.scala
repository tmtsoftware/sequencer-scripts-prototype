package aoesw
import csw.params.core.generics.KeyType
import csw.params.core.models.{Choice, Choices, Prefix}
import ocs.api.SequencerCommandService
import ocs.framework.ScriptImports._

class LGSAcquisition(csw: CswServices) extends Script(csw) {

  private val tcs = csw.sequencerCommandService("tcs")

  object aosq {
    val prefix = Prefix("aoesw.aosq")
    val name   = "ao-sequencer"
  }

  object oiwfsPoaAssembly {
    val prefix = Prefix("iris.oiwfs.poa")
    val name   = "oiwfs-poa-assembly"
  }

  object oiwfsDetectorAssembly {
    val prefix = Prefix("iris.oiwfs.detector")
    val name   = "oiwfs-detector-assembly"
  }

  object rtcAssembly {
    val prefix = Prefix("nfiraos.rtc")
    val name   = "nfiraos-rtc-assembly"
  }

  val oiwfsExposureModeChoices = Choices.from("SINGLE", "CONTINUOUS", "STOP", "NOOP")
  val oiwfsExposureModeKey     = KeyType.ChoiceKey.make("mode", oiwfsExposureModeChoices)

  val oiwfsStateEvent         = EventKey(rtcAssembly.prefix, EventName("oiwfsState"))
  val oiwfsStateEnableChoices = Choices.from("NONE", "TT", "TTF")
  val oiwfsStateEnableKey     = KeyType.ChoiceKey.make("enable", oiwfsStateEnableChoices)
  val oiwfsStateFluxHighKey   = KeyType.BooleanKey.make("fluxHigh")
  val oiwfsStateFluxLowKey    = KeyType.BooleanKey.make("fluxlow")

  val ttfOffsetEvent = EventKey(rtcAssembly.prefix, EventName("telOffloadTt")) // ??
  val ttfOffsetXKey  = KeyType.FloatKey.make("x")
  val ttfOffsetYKey  = KeyType.FloatKey.make("y")

  val tcsOffsetCoordinateSystemChoices = Choices.from("RADEC", "XY", "ALTAZ")
  val tcsOffsetCoordSystemKey          = KeyType.ChoiceKey.make("coordinateSystem", tcsOffsetCoordinateSystemChoices)
  val tcsOffsetXKey                    = KeyType.FloatKey.make("x")
  val tcsOffsetYKey                    = KeyType.FloatKey.make("y")
  val tcsOffsetVirtualTelescopeChoices =
    Choices.from("MOUNT", "OIWFS1", "OIWFS2", "OIWFS3", "OIWFS4", "ODGW1", "ODGW2", "ODGW3", "ODGW4", "GUIDER1", "GUIDER2")
  val tcsOffsetVTKey = KeyType.ChoiceKey.make("virtualTelescope", tcsOffsetVirtualTelescopeChoices)


  val loopEventKey = EventKey(rtcAssembly.prefix, EventName("loop"))
  val oiwfsLoopStatesChoices = Choices.from("IDLE", "LOST", "ACTIVE")
  val oiwfsLoopKey = KeyType.ChoiceKey.make("oiwfsPoa", oiwfsLoopStatesChoices)

  csw.subscribe(Set(loopEventKey)) {
    case ev: SystemEvent =>
      val oiwfsLoopStates = ev(oiwfsLoopKey)
      for (ii <- oiwfsLoopStates.items.indices) {
        if (oiwfsLoopStates.items(ii) == Choice("LOST")) {
          handleOiwfsLoopOpen(ii)
        }
      }
    Done
    case _ => Done
  }

  def handleOiwfsLoopOpen(oiwfsProbeNum: Int): Unit = {
    spawn {
      // Do something
      Done
    }
  }

  val TCSOFFSETTHRESHOLD = 2.0 // arcsec ???
  def isOffsetRequired(x: Float, y: Float): Boolean = Math.sqrt(x*x+y*y) > TCSOFFSETTHRESHOLD


  handleSetupCommand("enableOiwfsTtf") { command =>
    spawn {
      val ttfProbeNum = csw.get(oiwfsStateEvent).await match {
        case ev: SystemEvent => ev(oiwfsStateEnableKey).items.indexOf(Choice("TTF"))
        case _               => -1
      }

      var ttfFluxHigh = false
      var ttfFluxLow  = false
      var xoffset = 0.0f
      var yoffset = 0.0f
      val subscription = csw.subscribe(Set(oiwfsStateEvent, ttfOffsetEvent)) {
        case ev: SystemEvent =>
          ev.eventName match {
            case oiwfsStateEvent.eventName =>
              if (ttfProbeNum != -1) {
                ttfFluxHigh = ev(oiwfsStateFluxHighKey).value(ttfProbeNum)
                ttfFluxLow = ev(oiwfsStateFluxLowKey).value(ttfProbeNum)
              }
            case ttfOffsetEvent.eventName =>
              xoffset = ev(ttfOffsetXKey).head
              yoffset = ev(ttfOffsetYKey).head
            case _ =>
          }
          Done
        case _ => Done
      }

      // start continuous exposures on TTF probe
      val probeExpModes = (0 to 2).map(i => if (i == ttfProbeNum) Choice("CONTINUOUS") else Choice("NOOP"))
      val startExposureCommand = Setup(aosq.prefix, CommandName("exposure"), command.maybeObsId)
        .add(oiwfsExposureModeKey.set(probeExpModes:_*))

      csw.submitAndSubscribe(oiwfsDetectorAssembly.name, startExposureCommand).await match {

        case _: CommandResponse.Completed =>
          val guideStarLockedThreshold  = 5 // number of consecutive loops without an offset to consider stable
          var timesGuideStarLocked: Int = 0
          val maxAttempts               = 20 // maximum number of loops on this guide star before rejecting
          var attempts                  = 0
          loop(500.millis) { // period tbd
            spawn {
              if (ttfFluxLow) {
                // increase exposure time
                increaseExposureTime()
              } else {
                // gs found (centroid success)
                if (isOffsetRequired(xoffset, yoffset)) {
                  // offset telescope.  wait for completion
                  val offsetResponse = offsetTcs(tcs.await, xoffset, yoffset, ttfProbeNum, command.maybeObsId).await
                  timesGuideStarLocked = 0
                } else {
                  timesGuideStarLocked += 1
                }
              }
              attempts += 1
              stopWhen((timesGuideStarLocked == guideStarLockedThreshold) || (attempts == maxAttempts))
            }
          }.await

          if (timesGuideStarLocked == guideStarLockedThreshold) {
            csw.addOrUpdateCommand(CommandResponse.Completed(command.runId))
          } else {
            csw.addOrUpdateCommand(CommandResponse.Error(command.runId, "Guide Star Unstable"))
          }

        case x => csw.addOrUpdateCommand(CommandResponse.Error(command.runId, "Error starting WFS exposures: " + x))
      }
      subscription.unsubscribe()
      Done
    }
  }

  def increaseExposureTime(): Unit = {
    // not sure how this is done
  }
  def offsetTcs(tcs: SequencerCommandService, xoffset: Float, yoffset: Float, probeNum: Int, maybeObsId: Option[ObsId]) = {
    tcs.submit(
      Sequence(
        Setup(aosq.prefix, CommandName("offset"), maybeObsId)
          .add(tcsOffsetCoordSystemKey.set(Choice("RADEC")))
          .add(tcsOffsetXKey.set(xoffset))
          .add(tcsOffsetYKey.set(yoffset))
          .add(tcsOffsetVTKey.set(Choice(s"OIWFS$probeNum")))
      )
    )
  }

}
