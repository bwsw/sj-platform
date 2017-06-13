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
package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.exceptions._
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Creates message for [[com.bwsw.common.exceptions.JsonDeserializationException JsonDeserializationException]].
  *
  * @author Pavel Tomskikh
  */
class JsonDeserializationErrorMessageCreator(implicit injector: Injector) {

  def apply(exception: JsonDeserializationException,
            attributeRequiredMessage: String = "entity.error.attribute.required"): String = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

    createMessage(exception match {
      case _: JsonUnrecognizedPropertyException => "json.deserialization.error.unrecognized.property"
      case _: JsonIncorrectValueException => "json.deserialization.error.incorrect.value"
      case _: JsonNotParsedException => "json.deserialization.error.not.parsed"
      case _: JsonMissedPropertyException => attributeRequiredMessage
      case _ => "json.deserialization.error"
    }, exception.getMessage)
  }
}