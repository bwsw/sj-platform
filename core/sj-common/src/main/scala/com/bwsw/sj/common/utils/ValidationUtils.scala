package com.bwsw.sj.common.utils

/**
  * Useful methods for [[com.bwsw.sj.common.engine.StreamingValidator StreamingValidator]].
  *
  * @author Pavel Tomskikh
  */
object ValidationUtils {

  def isOptionStringField(string: Option[String]) = string.isEmpty || string.get.nonEmpty

  def isRequiredStringField(string: Option[String]) = string.nonEmpty && string.get.nonEmpty

  def checkFields(fields: Option[List[String]], uniqueKey: Option[List[String]], distribution: Option[List[String]]) = {
    fields match {
      case Some(fieldNames: List[String]) =>
        fieldNames.nonEmpty &&
          fieldNames.forall(x => isRequiredStringField(Option(x))) &&
          (uniqueKey.isEmpty || uniqueKey.get.forall(fieldNames.contains)) &&
          (distribution.isEmpty || distribution.get.forall(fieldNames.contains))
      case _ => false
    }
  }
}