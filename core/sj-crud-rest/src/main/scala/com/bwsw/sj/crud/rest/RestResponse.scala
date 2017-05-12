package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.rest.ResponseEntity
import com.bwsw.sj.common.rest.model.stream.StreamApi
import com.bwsw.sj.crud.rest.model.provider.ProviderApi
import com.bwsw.sj.crud.rest.model.service.ServiceApi

import scala.collection.mutable

case class ConnectionResponseEntity(connection: Boolean = true) extends ResponseEntity

case class TestConnectionResponseEntity(connection: Boolean, errors: String) extends ResponseEntity

case class ProviderResponseEntity(provider: ProviderApi) extends ResponseEntity

case class ProvidersResponseEntity(providers: mutable.Buffer[ProviderApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToProviderResponseEntity(services: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity


case class ServiceResponseEntity(service: ServiceApi) extends ResponseEntity

case class ServicesResponseEntity(services: mutable.Buffer[ServiceApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToServiceResponseEntity(streams: mutable.Buffer[String] = mutable.Buffer(),
                                          instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity


case class StreamResponseEntity(stream: StreamApi) extends ResponseEntity

case class StreamsResponseEntity(streams: mutable.Buffer[StreamApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToStreamResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity
