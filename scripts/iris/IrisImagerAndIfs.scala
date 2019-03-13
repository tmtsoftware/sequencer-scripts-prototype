package iris

import csw.params.core.generics.{GChoiceKey, Key, KeyType}
import csw.params.core.models.Choices
import ocs.framework.ScriptImports._

import scala.concurrent.{ExecutionContext, Future}

class IrisImagerAndIfs(csw: CswServices) extends Script(csw) {

  val isPrefix = Prefix("iris.is")

  val filterChoices: Choices = Choices.from("Z", "Y", "J", "H", "K", "Ks", "H+K notch", "CO",
    "BrGamma", "PaBeta", "H2", "FeII", "HeI", "CaII Trip", "J Cont", "H Cont", "K Cont",
    "Zn1", "Zn2", "Zn3", "Zn4", "Yn1", "Yn2", "Yn3", "Yn4", "Jn1", "Jn2", "Jn3", "Jn4", "Jn5",
    "Hn1", "Hn2", "Hn3", "Hn4", "Hn5", "Kn1", "Kn2", "Kn3", "Kn4", "Kn5")


  val scaleChoices: Choices = Choices.from("4", "9", "25", "50")
  val spectralResolutionChoices = Choices.from("4000-Z", "4000-Y", "4000-J", "4000-H", "4000-K",
    "4000-H+K", "8000-Z", "8000-Y", "8000-J", "8000-H", "8000-Kn1-3", "8000-Kn4-5", "8000-Kbb", "10000-Z",
    "10000-Y", "10000-J", "10000-H", "10000-K", "Mirror")


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

  val singleScaleKey: GChoiceKey      = KeyType.ChoiceKey.make("scale", scaleChoices)
  val singleResolutionKey: GChoiceKey = KeyType.ChoiceKey.make("spectralResolution", spectralResolutionChoices)

  handleSetupCommand("parallelObservation") { command =>
    spawn {
      // configure IRIS filter (and other upstream optics)
      val filter         = command(filterKey)
      val filterResponse = csw.submit("filterAssembly", Setup(isPrefix, CommandName("setFilter"), command.maybeObsId).add(filter))

      // configure IFS
      val scales      = command(scaleKey)
      val resolutions = command(resolutionKey)
      val scaleAndResolutionResponse = par {
        csw.submit("scaleAssembly",
                   Setup(isPrefix, CommandName("setScale"), command.maybeObsId)
                     .add(singleScaleKey.set(scales.head)))

        csw.submit(
          "resolutionAssembly",
          Setup(isPrefix, CommandName("setResolution"), command.maybeObsId)
            .add(singleResolutionKey.set(resolutions.head))
        )
      }

      // exposure parameters
      val imagerItime   = command(imagerItimeKey).head
      val imagerRamps   = command(imagerRampsKey).head
      val imagerRepeats = command(imagerRepeatsKey).head

      val numIfsConfigs = command(ifsConfigurationsKey).head
      val ifsItimes     = command(ifsItimeKey)
      val ifsRamps      = command(ifsRampsKey)
      val ifsRepeats    = command(ifsRepeatsKey)

      val response = filterResponse.await
      // start Imager Exposure loop
      // Imager loop
      var imagerExposureCounter = 0
      val imagerExposureLoop = loop {
        spawn {
          // configure exposure
          val imagerItimeKey = KeyType.IntKey.make("integrationTime")
          val imagerRampsKey = KeyType.IntKey.make("ramps")
          csw
            .submit(
              "imagerDetectorAssembly",
              Setup(isPrefix, CommandName("configureExposure"), command.maybeObsId)
                .add(imagerItimeKey.set(ifsItimes.get(imagerExposureCounter).get))
                .add(imagerRampsKey.set(ifsRamps.get(imagerExposureCounter).get))
            )
            .await
          // observe (assumes completes when exposure is complete)
          csw
            .submit("imagerDetectorAssembly", Observe(isPrefix, CommandName("observe"), command.maybeObsId))
            .await

          imagerExposureCounter += 1

          stopWhen(imagerExposureCounter == imagerRepeats)
        }
      }

      scaleAndResolutionResponse.await

      // start IFS Exposure Loop
      var ifsConfigurationCounter = 0
      val ifsExposureLoop = loop {
        spawn {
          if (ifsConfigurationCounter > 1) {
            // configure IFS settings
            val ifsScaleKey      = KeyType.ChoiceKey.make("scale", scaleChoices)
            val ifsResolutionKey = KeyType.ChoiceKey.make("spectralResolution", spectralResolutionChoices)

            par {
              csw.submit(
                "scaleAssembly",
                Setup(isPrefix, CommandName("setScale"), command.maybeObsId)
                  .add(ifsScaleKey.set(scales.get(ifsConfigurationCounter).get))
              )

              csw.submit(
                "resolutionAssembly",
                Setup(isPrefix, CommandName("setResolution"), command.maybeObsId)
                  .add(ifsResolutionKey.set(resolutions.get(ifsConfigurationCounter).get))
              )
            }.await
          }

          var ifsExposureCounter = 0
          loop {
            spawn {
              // configure exposure
              val ifsItimeKey = KeyType.IntKey.make("integrationTime")
              val ifsRampsKey = KeyType.IntKey.make("ramps")
              csw
                .submit(
                  "ifsDetectorAssembly",
                  Setup(isPrefix, CommandName("configureExposure"), command.maybeObsId)
                    .add(ifsItimeKey.set(ifsItimes.get(ifsExposureCounter).get))
                    .add(ifsRampsKey.set(ifsRamps.get(ifsExposureCounter).get))
                )
                .await
              // observe (assumes completes when exposure is complete)
              csw.submit("ifsDetetorAssembly", Observe(isPrefix, CommandName("observe"), command.maybeObsId)).await

              ifsExposureCounter += 1

              stopWhen(ifsExposureCounter == ifsRepeats.get(ifsExposureCounter).get)
            }

          }.await

          ifsConfigurationCounter += 1

          stopWhen(ifsConfigurationCounter == numIfsConfigs)
        }
      }

      // if results are successful, updated CRM
      csw.addOrUpdateCommand(CommandResponse.Completed(command.runId))
      Done
    }
  }

  override def onShutdown(): Future[Done] = spawn {
    println("shutdown iris")
    Done
  }

  override def abort(): Future[Done] = spawn {
    Done
  }
}
