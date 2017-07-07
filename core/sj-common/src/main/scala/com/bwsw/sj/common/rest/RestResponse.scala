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
package com.bwsw.sj.common.rest

import com.bwsw.sj.common.config.ConfigLiterals
import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.field
import scala.collection.mutable

class RestResponse(@(JsonProperty @field)("status-code") val statusCode: Int = 0, entity: ResponseEntity = new ResponseEntity {})

case class OkRestResponse(var entity: ResponseEntity) extends RestResponse(200, entity)

case class CreatedRestResponse(var entity: ResponseEntity) extends RestResponse(201, entity)

case class BadRequestRestResponse(var entity: ResponseEntity) extends RestResponse(400, entity)

case class NotFoundRestResponse(var entity: ResponseEntity) extends RestResponse(404, entity)

case class ConflictRestResponse(var entity: ResponseEntity) extends RestResponse(409, entity)

case class UnprocessableEntityRestResponse(var entity: ResponseEntity) extends RestResponse(422, entity)

case class InternalServerErrorRestResponse(var entity: ResponseEntity) extends RestResponse(500, entity)


trait ResponseEntity

case class FrameworkTask(id: String,
                         state: String,
                         stateChange: String,
                         reason: String,
                         node: String,
                         lastNode: String,
                         directories: scala.collection.mutable.ListBuffer[Directory])

case class Directory(name: String, path: String)

case class FrameworkRestEntity(tasks: Seq[FrameworkTask]) extends ResponseEntity

case class MessageResponseEntity(message: String) extends ResponseEntity

case class KeyedMessageResponseEntity(message: String, key: String) extends ResponseEntity


case class TypesResponseEntity(types: Seq[String]) extends ResponseEntity

case class RelatedToStreamResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

case class DomainsResponseEntity(domains: Seq[String] = ConfigLiterals.domains) extends ResponseEntity
