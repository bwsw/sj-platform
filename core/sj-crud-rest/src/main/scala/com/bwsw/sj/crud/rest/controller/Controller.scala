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
package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.ServiceInterface
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.result.{Deleted, DeletionError, EntityNotFound}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import scaldi.Injectable.inject
import scaldi.Injector

trait Controller {
  protected implicit val injector: Injector
  protected val serializer: JsonSerializer = inject[JsonSerializer]
  serializer.enableNullForPrimitives(false)
  protected val jsonDeserializationErrorMessageCreator = inject[JsonDeserializationErrorMessageCreator]
  protected val serviceInterface: ServiceInterface[_, _]

  protected val entityDeletedMessage: String
  protected val entityNotFoundMessage: String

  def create(serializedEntity: String): RestResponse

  def getAll(): RestResponse

  def get(name: String): RestResponse

  def delete(name: String): RestResponse = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

    serviceInterface.delete(name) match {
      case Deleted =>
        OkRestResponse(MessageResponseEntity(createMessage(entityDeletedMessage, name)))
      case EntityNotFound =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
      case DeletionError(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }
  }
}
