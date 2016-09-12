package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.ZKService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

import scala.collection.mutable.ArrayBuffer

class ZKCoordServiceData() extends ServiceData() {
  serviceType = "ZKCoord"
  var namespace: String = null
  var provider: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new ZKService()
    super.fillModelService(modelService)
    modelService.provider = providerDAO.get(this.provider).get
    modelService.namespace = this.namespace

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'namespace' field
    errors ++= validateStringFieldRequired(this.namespace, "Namespace")
    errors ++= validateNamespace(this.namespace)

    errors
  }
}
