package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.rest.ResponseEntity
import com.bwsw.sj.crud.rest.dto.provider.ProviderData

import scala.collection.mutable

case class ConnectionResponseEntity(connection: Boolean = true) extends ResponseEntity

case class TestConnectionResponseEntity(connection: Boolean, errors: String) extends ResponseEntity

case class ProviderResponseEntity(provider: ProviderData) extends ResponseEntity

case class ProvidersResponseEntity(providers: mutable.Buffer[ProviderData] = mutable.Buffer()) extends ResponseEntity

case class RelatedToProviderResponseEntity(services: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

