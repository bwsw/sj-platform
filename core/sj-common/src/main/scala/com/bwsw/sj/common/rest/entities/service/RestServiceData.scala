package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.RestService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer

/**
  *
  * @author Pavel Tomskikh
  */
class RestServiceData extends ServiceData {
  serviceType = ServiceLiterals.restType
  var provider: String = _
  var basePath: String = _
  var httpVersion: String = _

  override def asModelService = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new RestService
    modelService.provider = providerDAO.get(provider).get
    modelService.basePath = basePath
    modelService.httpVersion = httpVersion
    modelService
  }

  override def validate() = {
    val basePathAttributeName = "basePath"
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

    errors
  }


}
