package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.service.RestService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

/**
  * @author Pavel Tomskikh
  */
class RestServiceData(
                       override val name: String,
                       val provider: String,
                       val basePath: String = "/",
                       val httpVersion: String = RestLiterals.http_1_1,
                       val headers: Map[String, String] = Map(),
                       override val description: String = ServiceLiterals.defaultDescription)
  extends ServiceData(ServiceLiterals.restType, name, description) {

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new RestService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.basePath,
      RestLiterals.httpVersionFromString(this.httpVersion),
      this.headers.asJava
    )

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
        if (!RestLiterals.httpVersions.contains(x))
          errors += createMessage(
            "entity.error.attribute.must.one_of",
            httpVersionAttributeName,
            RestLiterals.httpVersions.mkString("[", ", ", "]"))
    }

    errors
  }
}
