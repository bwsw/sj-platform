package com.bwsw.sj.common.utils

/**
  * Useful methods for [[com.bwsw.sj.common.engine.StreamingValidator StreamingValidator]].
  *
  * @author Pavel Tomskikh
  */
object ValidationUtils {

  def isRequired(p: Any => Boolean)(options: Map[String, Any], key: String) = {
    val value = options.get(key)
    value.nonEmpty && p(value.get)
  }

  def isOption(p: Any => Boolean)(options: Map[String, Any], key: String) = {
    val value = options.get(key)
    value.isEmpty || p(value.get)
  }

  def isString(a: Any) = a.isInstanceOf[String] && a.asInstanceOf[String].nonEmpty

  def compareFieldNames(options: Map[String, Any], fieldNames: Seq[Any])(checking: String) = {
    options.get(checking) match {
      case Some(checkingFields: Seq[Any]) =>
        checkingFields.nonEmpty && checkingFields.forall(fieldNames.contains)
      case None => true
      case _ => false
    }
  }

  def checkAvroFields(options: Map[String, Any], allFields: String, optionFields: Seq[String]) = {
    val fieldsValue = options.get(allFields)
    fieldsValue match {
      case Some(fieldNames: Seq[Any]) =>
        fieldNames.nonEmpty &&
          fieldNames.forall(isString) &&
          optionFields.forall(compareFieldNames(options, fieldNames))
      case _ => false
    }
  }
}
