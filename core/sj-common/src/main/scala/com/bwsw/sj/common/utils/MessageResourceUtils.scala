package com.bwsw.sj.common.utils

import java.text.MessageFormat
import java.util.ResourceBundle

trait MessageResourceUtils {
  private val messages = ResourceBundle.getBundle("messages")

  def createMessage(name: String, params: String*) = {
    MessageFormat.format(getMessage(name), params: _*)
  }

  def getMessage(name: String) = {
    messages.getString(name)
  }
}