package ocs

import csw.params.core.generics.{GChoiceKey, Key, KeyType}
import csw.params.core.models.{Choice, Choices, Units}
import ocs.framework.ScriptImports._

class OcsIrisImagerAndIfs(csw: CswServices) extends Script(csw) {

  val filterChoices: Choices = Choices.from(
    "Z",
    "Y",
    "J",
    "H",
    "K",
    "Ks",
    "H+K notch",
    "CO",
    "BrGamma",
    "PaBeta",
    "H2",
    "FeII",
    "HeI",
    "CaII Trip",
    "J Cont",
    "H Cont",
    "K Cont",
    "Zn1",
    "Zn2",
    "Zn3",
    "Zn4",
    "Yn1",
    "Yn2",
    "Yn3",
    "Yn4",
    "Jn1",
    "Jn2",
    "Jn3",
    "Jn4",
    "Jn5",
    "Hn1",
    "Hn2",
    "Hn3",
    "Hn4",
    "Hn5",
    "Kn1",
    "Kn2",
    "Kn3",
    "Kn4",
    "Kn5"
  )

  val scaleChoices: Choices = Choices.from("4", "9", "25", "50")
  val spectralResolutionChoices = Choices.from(
    "4000-Z",
    "4000-Y",
    "4000-J",
    "4000-H",
    "4000-K",
    "4000-H+K",
    "8000-Z",
    "8000-Y",
    "8000-J",
    "8000-H",
    "8000-Kn1-3",
    "8000-Kn4-5",
    "8000-Kbb",
    "10000-Z",
    "10000-Y",
    "10000-J",
    "10000-H",
    "10000-K",
    "Mirror"
  )

  val filterKey: GChoiceKey          = KeyType.ChoiceKey.make("filter", filterChoices)
  val scaleKey: GChoiceKey           = KeyType.ChoiceKey.make("scale", scaleChoices)
  val resolutionKey: GChoiceKey      = KeyType.ChoiceKey.make("spectralResolution", spectralResolutionChoices)
  val imagerItimeKey: Key[Int]       = KeyType.IntKey.make("imagerIntegrationTime")
  val imagerRampsKey: Key[Int]       = KeyType.IntKey.make("imagerNumRamps")
  val imagerRepeatsKey: Key[Int]     = KeyType.IntKey.make("imagerNumRepeats")
  val ifsItimeKey: Key[Int]          = KeyType.IntKey.make("ifsIntegrationTime")
  val ifsRampsKey: Key[Int]          = KeyType.IntKey.make("ifsNumRamps")
  val ifsRepeatsKey: Key[Int]        = KeyType.IntKey.make("ifsNumRepeats")
  val ifsConfigurationsKey: Key[Int] = KeyType.IntKey.make("ifsConfigurations")

  handleSetupCommand("irisImagerAndIfs") { command =>
    spawn {
      // The following settings would be passed in command parameters
      // but the extraction of these values is omitted here for brevity.
      val filter            = Choice("K")
      val resolutions       = Array(Choice("4000-K"), Choice("4000-K"))
      val scales            = Array(Choice("4"), Choice("9"))
      val imagerItime       = 60
      val imagerNumRamps    = 1
      val imagerRepeats     = 2
      val ifsItimes         = Array(2, 2)
      val ifsNumRamps       = Array(1, 1)
      val ifsRepeats        = Array(16, 16)
      val ifsConfigurations = 2

      // send entire setup to IRIS seq
      val irisCommand = Setup(Prefix("esw.ocs"), CommandName("parallelObservation"), command.maybeObsId)
        .add(filterKey.set(filter))
        .add(scaleKey.set(scales))
        .add(resolutionKey.set(resolutions))
        .add(imagerItimeKey.set(imagerItime).withUnits(Units.millisecond))
        .add(imagerRampsKey.set(imagerNumRamps))
        .add(ifsItimeKey.set(ifsItimes).withUnits(Units.millisecond))
        .add(ifsRampsKey.set(ifsNumRamps))
        .add(ifsRepeatsKey.set(ifsRepeats))
        .add(ifsConfigurationsKey.set(ifsConfigurations))

      val response = csw
        .sequencerCommandService("iris")
        .await
        .submit(Sequence(irisCommand))
        .await

      /**
       * directly adding or updating response would be a wrong approach,
       * as the response would have command ID of irisCommand(line 102)
       * which is a different command then the original command received.
       *
       * right approach could be -
       * csw.addOrUpdateCommand(CommandResponse.withRunId(command.runId,response))
       *
       * or since you are passing a sequence downstream you can also do
       * csw.addSequenceResponse(Set(command),response)
       *
       * or you can also add irisCommand as subcommand to original command and then
       * give the response directly to CRM, since it will first mark the subcommand complete and
       * parent command would be inferred automatically
       *
       * csw.addSubCommands(command, Set(irisCommand))
       * csw.updateSubCommand(response)
       */
      csw.addOrUpdateCommand(CommandResponse.withRunId(command.runId, response))
      Done
    }
  }

  handleSetupCommand("irisImagerAndIfsSeparate") { command =>
    spawn {
      // The following settings would be passed in command parameters
      // but the extraction of these values is omitted here for brevity.
      val filter            = Choice("K")
      val resolutions       = Array(Choice("4000-K"), Choice("4000-K"))
      val scales            = Array(Choice("4"), Choice("9"))
      val imagerItime       = 60
      val imagerNumRamps    = 1
      val imagerRepeats     = 2
      val ifsItimes         = Array(2, 2)
      val ifsNumRamps       = Array(1, 1)
      val ifsRepeats        = Array(16, 16)
      val ifsConfigurations = 2

      val irisImagerSeq = csw.sequencerCommandService("irisImager").await
      val irisIfsSeq    = csw.sequencerCommandService("irisIfs").await

      val ocsPrefix = Prefix("esw.ocs")

      def repeat(command: Observe, times: Int) = (1 to times).map(_ => command.cloneCommand)

      // send filter command
      val imagerConfigResponse = irisImagerSeq.submit(
        Sequence(
          Setup(ocsPrefix, CommandName("configureImager"), command.maybeObsId)
            .add(filterKey.set(filter))
        )
      )

      // send IFS setup
      val ifsConfigResponse = irisIfsSeq.submit(
        Sequence(
          Setup(ocsPrefix, CommandName("configureIfs"), command.maybeObsId)
            .add(scaleKey.set(scales))
            .add(resolutionKey.set(resolutions))
        )
      )

      // wait for filter to finish moving
      imagerConfigResponse.await

      // send Imager sequence
      val imagerObserveCommand = Observe(ocsPrefix, CommandName("observe"), command.maybeObsId)
        .add(imagerItimeKey.set(imagerItime))
        .add(imagerRampsKey.set(imagerNumRamps))

      val imagerObserveSequence = Sequence(repeat(imagerObserveCommand, imagerRepeats): _*)
      val imagerObserveResponse = irisImagerSeq.submit(imagerObserveSequence)

      // send IFS sequence
      val firstIfsObserveCommand = Observe(ocsPrefix, CommandName("observe"), command.maybeObsId)
        .add(ifsItimeKey.set(ifsItimes.head))
        .add(ifsRampsKey.set(ifsNumRamps.head))
      val ifsObserveSequence = Sequence(repeat(firstIfsObserveCommand, ifsRepeats.head): _*)

      (1 to ifsConfigurations - 1).foreach { configNum =>
        val nextScaleKey: GChoiceKey      = KeyType.ChoiceKey.make("scale", scaleChoices)
        val nextResolutionKey: GChoiceKey = KeyType.ChoiceKey.make("spectralResolution", spectralResolutionChoices)

        val nextSetup = Setup(ocsPrefix, CommandName("configureIfs"), command.maybeObsId)
          .add(nextScaleKey.set(scales(configNum)))
          .add(nextResolutionKey.set(resolutions(configNum)))
        ifsObserveSequence.add(Sequence(nextSetup))

        val nextIfsItimeKey = KeyType.IntKey.make("ifsIntegrationTime")
        val nextIfsRampsKey = KeyType.IntKey.make("ifsNumRamps")

        val nextObserveCommand = Observe(ocsPrefix, CommandName("observe"), command.maybeObsId)
          .add(ifsItimeKey.set(ifsItimes(configNum)))
          .add(ifsRampsKey.set(ifsNumRamps(configNum)))

        ifsObserveSequence.add(Sequence(repeat(nextObserveCommand, ifsRepeats(configNum)): _*))
      }
      val ifsObserveResponse = irisIfsSeq.submit(ifsObserveSequence).await
      imagerObserveResponse.await

      csw.addOrUpdateCommand(CommandResponse.Completed(command.runId))
      Done
    }
  }

}
