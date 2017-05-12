package com.bwsw.sj.common.utils

import java.text.MessageFormat
import java.util.ResourceBundle

import scala.collection.mutable.ArrayBuffer

object MessageResourceUtils {
  private val messages = ResourceBundle.getBundle("messages")

  def createMessage(name: String, params: String*): String =
    MessageFormat.format(getMessage(name), params: _*)

  def createMessageWithErrors(name: String, errors: ArrayBuffer[String]): String =
    createMessage(name, errors.mkString(";"))

  def getMessage(name: String): String = messages.getString(name)
}