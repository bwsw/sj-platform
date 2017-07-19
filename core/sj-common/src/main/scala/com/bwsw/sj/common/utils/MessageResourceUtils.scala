/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.utils

import java.text.MessageFormat
import java.util.ResourceBundle

import scala.collection.mutable.ArrayBuffer

/**
  * Wrapper for [[java.util.ResourceBundle]] of rest messages
  */

class MessageResourceUtils {
  private val messages = ResourceBundle.getBundle("messages")

  def createMessage(name: String, params: String*): String =
    MessageFormat.format(getMessage(name), params: _*)

  def createMessageWithErrors(name: String, errors: ArrayBuffer[String]): String =
    createMessage(name, errors.mkString(";"))

  def getMessage(name: String): String = messages.getString(name)
}
