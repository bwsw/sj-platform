package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonProperty

class RestResponse(@JsonProperty("status-code") var statusCode: Int, entity: Map[String, Any])

case class OkRestResponse(var entity: Map[String, Any]) extends RestResponse(200, entity)

case class CreatedRestResponse(var entity: Map[String, Any]) extends RestResponse(201, entity)

case class BadRequestRestResponse(var entity: Map[String, Any]) extends RestResponse(400, entity)

case class NotFoundRestResponse(var entity: Map[String, Any]) extends RestResponse(404, entity)

case class ConflictRestResponse(var entity: Map[String, Any]) extends RestResponse(409, entity)

case class UnprocessableEntityRestResponse(var entity: Map[String, Any]) extends RestResponse(422, entity)

case class InternalServerErrorRestResponse(var entity: Map[String, Any]) extends RestResponse(500, entity)
