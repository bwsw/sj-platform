package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.TStreamService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ProviderLiterals, ServiceLiterals}

import scala.collection.mutable.ArrayBuffer

class TstrQServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.tstreamsType
  var metadataProvider: String = null
  var metadataNamespace: String = null
  var dataProvider: String = null
  var dataNamespace: String = null
  var lockProvider: String = null
  var lockNamespace: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new TStreamService()
    super.fillModelService(modelService)
    modelService.metadataProvider = providerDAO.get(this.metadataProvider).get
    modelService.metadataNamespace = this.metadataNamespace
    modelService.dataProvider = providerDAO.get(this.dataProvider).get
    modelService.dataNamespace = this.dataNamespace
    modelService.lockProvider = providerDAO.get(this.lockProvider).get
    modelService.lockNamespace = this.lockNamespace

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    errors ++= super.validateGeneralFields()

    // 'metadataProvider' field
    Option(this.metadataProvider) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Metadata-provider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Metadata-provider")
        }
        else {
          val metadataProviderObj = providerDAO.get(x)
          metadataProviderObj match {
            case None =>
              errors += createMessage("entity.error.doesnot.exist", "Metadata-provider", x)
            case Some(provider) =>
              if (provider.providerType != ProviderLiterals.cassandraType) {
                errors += createMessage("entity.error.must.one.type.other.given", "Metadata-provider", ProviderLiterals.cassandraType, provider.providerType)
              }
          }
        }
    }

    // 'metadataNamespace' field
    Option(this.metadataNamespace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Metadata-namespace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Metadata-namespace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "metadata-namespace", x)
          }
        }
    }

    // 'dataProvider' field
    Option(this.dataProvider) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Data-provider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Data-provider")
        } else {
          val dataProviderObj = providerDAO.get(x)
          val allowedTypes = List(ProviderLiterals.cassandraType, ProviderLiterals.aerospikeType)
          dataProviderObj match {
            case None =>
              errors += s"Data-provider '$x' does not exist"
            case Some(provider) =>
              if (!allowedTypes.contains(provider.providerType)) {
                errors += createMessage("entity.error.must.one.type.other.given", "Data-provider", allowedTypes.mkString("' or '"), provider.providerType)
              }
          }
        }
    }

    // 'dataNamespace' field
    Option(this.dataNamespace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Data-namespace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Data-namespace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "data-namespace", x)
          }
        }
    }

    Option(this.lockProvider) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Lock-provider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Lock-provider")
        } else {
          val lockProviderObj = providerDAO.get(x)
          lockProviderObj match {
            case None =>
              errors += s"Lock-provider '$x' does not exist"
            case Some(provider) =>
              if (provider.providerType != ProviderLiterals.zookeeperType) {
                errors += createMessage("entity.error.must.one.type.other.given", "Data-provider", ProviderLiterals.zookeeperType, provider.providerType)
              }
          }
        }
    }

    // 'lockNamespace' field
    Option(this.lockNamespace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Lock-namespace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Lock-namespace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "lock-namespace", x)
          }
        }
    }

    errors
  }
}
