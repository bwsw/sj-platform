package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonProperty

case class Response(statusCode: Int, key: String, message: String)

case class ProtocolResponse(@JsonProperty("status-code") var statusCode: Int, var entity: Map[String, Any])
