package iris
import csw.params.core.generics.KeyType
import iris.IrisConstants._
import tmt.ocs.ScriptImports._

class ReactiveMonitoringExample(csw: CswServices) extends Script(csw) {

  private val notableEvent = EventKey(cryoenvAssembly.prefix, EventName("motorTempTooHighNotableEvent"))
  private val powerStatusKey = KeyType.BooleanKey.make("powerStatus").set(false)
  private val powerSetup = Setup(is.prefix, CommandName("setMotorPower"), None).add(powerStatusKey)
  csw.subscribe(Set(notableEvent)) {
    case ev: SystemEvent =>
      csw.submit("MotorAssembly", powerSetup).await
      Done
  }
}
