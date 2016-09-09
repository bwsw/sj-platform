package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.AerospikeService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

import scala.collection.mutable.ArrayBuffer

class ArspkDBServiceData() extends ServiceData() {
  serviceType = "ArspkDB"
  var namespace: String = null
  var provider: String = null

  override def toModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new AerospikeService()
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
