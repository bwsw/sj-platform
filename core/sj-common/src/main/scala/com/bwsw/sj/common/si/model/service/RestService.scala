package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateProvider
import com.bwsw.sj.common.utils.RestLiterals
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * @author Pavel Tomskikh
  */
class RestService(name: String,
                  val provider: String,
                  val basePath: String,
                  val httpVersion: String,
                  val headers: Map[String, String],
                  description: String,
                  serviceType: String)
                 (implicit injector: Injector)
  extends Service(serviceType, name, description) {

  import messageResourceUtils.createMessage

  override def to(): RestServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new RestServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        basePath = this.basePath,
        httpVersion = RestLiterals.httpVersionFromString(this.httpVersion),
        headers = this.headers.asJava
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
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
