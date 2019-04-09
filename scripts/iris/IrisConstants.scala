package iris

import csw.params.core.generics.{GChoiceKey, Key, KeyType}
import csw.params.core.models.{Choices, Prefix}
import csw.params.events.EventName
import helpers.ScriptUtilities._

object IrisConstants {

  val samplingModeChoices: Choices = makeChoices("CDS", "MCD", "UTR")

  def imagerFilterPositions(filter: String): List[String] = filter match {
    case "H" => List("H", "OPEN", "OPEN", "OPEN", "OPEN")
    case "J" => List("J", "OPEN", "OPEN", "OPEN", "OPEN")
    case "K" => List("K", "OPEN", "OPEN", "OPEN", "OPEN")
    case "CO" => List("OPEN", "OPEN", "OPEN", "OPEN", "CO")
    case "Hn1" => List("OPEN", "Hn1", "OPEN", "OPEN", "OPEN")
    case _ => List.empty // invalid (how to handle?)
  }


  object is {
    val prefix = Prefix("iris.ici.is")
    val name = "instrument-sequencer"
    val filterChoices: Choices = makeChoices("Z", "Y", "J", "H", "K", "Ks", "H+K notch", "CO",
      "BrGamma", "PaBeta", "H2", "FeII", "HeI", "CaII Trip", "J Cont", "H Cont", "K Cont",
      "Zn1", "Zn2", "Zn3", "Zn4", "Yn1", "Yn2", "Yn3", "Yn4", "Jn1", "Jn2", "Jn3", "Jn4", "Jn5",
      "Hn1", "Hn2", "Hn3", "Hn4", "Hn5", "Kn1", "Kn2", "Kn3", "Kn4", "Kn5")
    val imagerObserveChoices: Choices = makeChoices("START", "STOP", "ABORT")

    object command {
      val filterKey: GChoiceKey = KeyType.ChoiceKey.make("filter", filterChoices)
      val itimeKey: Key[Int] = KeyType.IntKey.make("imagerIntegrationTime")
      val rampsKey: Key[Int] = KeyType.IntKey.make("imagerNumRamps")

      val imagerObserveKey: GChoiceKey = KeyType.ChoiceKey.make("imagerObserve", imagerObserveChoices)
    }
    object event {
      val observerKeywordsEvent = EventName("observerKeywords")
    }
  }

  object sciFilterAssembly {
    val prefix = Prefix("iris.imager.filter")
    val name = "sci-filter-assembly"
    val filterWheelNames = List("wheel1", "wheel2", "wheel3", "wheel4", "wheel5")

    object command {
      val wheelKeys: List[Key[String]] = filterWheelNames.map(w => KeyType.StringKey.make(w)) // really an enum key
    }
    object event { }
  }

  object imagerDetectorAssembly {
    val prefix = Prefix("iris.imager.detector")
    val name = "imager-detector-assembly"

    object command {
      val setupImagerItimeKey: Key[Int] = KeyType.IntKey.make("rampIntegrationTime")
      val setupImagerRampKey: Key[Int] = KeyType.IntKey.make("ramps")
      val setupImagerSamplingModeKey: GChoiceKey = KeyType.ChoiceKey.make("imagerSamplingMode", samplingModeChoices)
    }
    object event {
      val exposureInProgressKey: Key[Boolean] = KeyType.BooleanKey.make("exposureInProgress")

    }
  }
  object cryoenvAssembly {
    val prefix = Prefix("iris.sc.cryoenv")
    val name = "cryoenv-assembly"
    val cryoenvStateEventNames: Set[String] = Set("IMG_STATE", "IFS_STATE", "WIN_STATE", "PV_STATE", "PRESS_STATE")
    val cryoenvVacuumStates: Choices = makeChoices("WARM", "PUMPING", "WARM_VACUUM", "COOLING", "COLD", "WARMING", "PRESSURIZING")

    object command { }
    object event {
      val cryoenvVacuumStateKey: GChoiceKey = KeyType.ChoiceKey.make("thermalVacuumState", cryoenvVacuumStates)
    }
  }
}
