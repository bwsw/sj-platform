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
                         directories: Seq[String])

case class FrameworkRestEntity(tasks: Seq[FrameworkTask]) extends ResponseEntity

case class MessageResponseEntity(message: String) extends ResponseEntity

case class KeyedMessageResponseEntity(message: String, key: String) extends ResponseEntity


case class TypesResponseEntity(types: Seq[String]) extends ResponseEntity

case class RelatedToStreamResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

case class DomainsResponseEntity(domains: Seq[String] = ConfigLiterals.domains) extends ResponseEntity
