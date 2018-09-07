package helpers

import csw.messages.params.models.{Choice, Choices}

object ScriptUtilities {
  def makeChoices(str: String*): Choices = {
    Choices(str.map(Choice(_)).toSet)
  }
}
