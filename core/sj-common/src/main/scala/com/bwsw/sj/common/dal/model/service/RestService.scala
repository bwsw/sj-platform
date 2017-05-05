package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.Provider
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{PropertyField, ReferenceField}
import com.bwsw.sj.common.rest.model.service.RestServiceData
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import org.eclipse.jetty.http.HttpVersion

import scala.collection.JavaConverters._

/**
  * Service for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestService(override val name: String,
                  override val description: String,
                  @ReferenceField val provider: Provider,
                  @PropertyField("base-path") val basePath: String,
                  @PropertyField("http-version") val httpVersion: HttpVersion,
                  val headers: java.util.Map[String, String],
                  override val serviceType: String = ServiceLiterals.restType)
  extends Service(name, description, serviceType) {

  override def asProtocolService: RestServiceData = {
    val protocolService = new RestServiceData
    super.fillProtocolService(protocolService)
    protocolService.provider = provider.name
    protocolService.basePath = basePath
    protocolService.httpVersion = RestLiterals.httpVersionToString(httpVersion)
    protocolService.headers = Map(headers.asScala.toList: _*)

    protocolService
  }
}
