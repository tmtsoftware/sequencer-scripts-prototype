package helpers
import csw.params.core.models.{Choice, Choices}

object ScriptUtilities {
  def makeChoices(str: String*): Choices = {
    Choices(str.map(Choice(_)).toSet)
  }
}
