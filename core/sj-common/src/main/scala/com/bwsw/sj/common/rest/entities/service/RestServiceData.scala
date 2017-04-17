package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.RestService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals
import org.eclipse.jetty.http.HttpVersion

import scala.collection.mutable.ArrayBuffer

/**
  * Service for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestServiceData extends ServiceData {
  serviceType = ServiceLiterals.restType
  var provider: String = _
  var basePath: String = "/"
  var httpVersion: String = "HTTP/1.1"

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new RestService
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(provider).get
    modelService.basePath = basePath
    modelService.httpVersion = httpVersion
    modelService
  }

  override def validate() = {
    val basePathAttributeName = "basePath"
    val httpVersionAttributeName = "httpVersion"
    val errors = new ArrayBuffer[String]()
    errors ++= super.validate()

    // 'provider' field
    errors ++= validateProvider(provider, serviceType)

    // 'basePath' field
    Option(basePath) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", basePathAttributeName)
      case Some(x) =>
        if (!x.startsWith("/"))
          errors += createMessage("entity.error.attribute.must", basePathAttributeName, "starts with '/'")
      case _ =>
    }

    // 'httpVersion' field
    Option(httpVersion) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", httpVersionAttributeName)
      case Some(x) =>
        if (HttpVersion.fromString(x) == null)
          errors += createMessage(
            "entity.error.attribute.must.one_of",
            httpVersionAttributeName,
            HttpVersion.values.mkString("[", ", ", "]"))
    }

    errors
  }


}
