package com.bwsw.sj.common.DAL.model

import java.util

import com.bwsw.sj.common.rest.entities.service.RestServiceData
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import org.eclipse.jetty.http.HttpVersion
import org.mongodb.morphia.annotations.Reference

import scala.collection.JavaConverters._

/**
  * Service for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestService extends Service {
  serviceType = ServiceLiterals.restType
  @Reference var provider: Provider = _
  var basePath: String = _
  var httpVersion: HttpVersion = _
  var headers: java.util.Map[String, String] = new util.HashMap[String, String]()

  def this(
      name: String,
      serviceType: String,
      description: String,
      provider: Provider,
      basePath: String,
      httpVersion: HttpVersion,
      headers: java.util.Map[String, String]) = {
    this
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.basePath = basePath
    this.httpVersion = httpVersion
    this.headers = headers
  }

  override def asProtocolService = {
    val protocolService = new RestServiceData
    super.fillProtocolService(protocolService)
    protocolService.provider = provider.name
    protocolService.basePath = basePath
    protocolService.httpVersion = RestLiterals.httpVersionToString(httpVersion)
    protocolService.headers = Map(headers.asScala.toList: _*)

    protocolService
  }
}
