package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.TStreamService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceLiterals, ProviderLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ArrayBuffer

class TstrQServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.tstreamsType
  @JsonProperty("metadata-provider") var metadataProvider: String = null
  @JsonProperty("metadata-namespace") var metadataNamespace: String = null
  @JsonProperty("data-provider") var dataProvider: String = null
  @JsonProperty("data-namespace") var dataNamespace: String = null
  @JsonProperty("lock-provider") var lockProvider: String = null
  @JsonProperty("lock-namespace") var lockNamespace: String = null

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
        errors += "'Metadata-provider' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += "'Metadata-provider' is required"
        }
        else {
          val metadataProviderObj = providerDAO.get(x)
          metadataProviderObj match {
            case None =>
              errors += s"Metadata-provider '$x' does not exist"
            case Some(provider) =>
              if (provider.providerType != ProviderLiterals.cassandraType) {
                errors += s"'Metadata-provider' must be of type '${ProviderLiterals.cassandraType}' " +
                  s"('${provider.providerType}' is given instead)"
              }
          }
        }
    }

    // 'metadataNamespace' field
    Option(this.metadataNamespace) match {
      case None =>
        errors += "'Metadata-namespace' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += "'Metadata-namespace' is required"
        }
        else {
          if (!validateNamespace(x)) {
            errors += s"Service has incorrect 'metadata-namespace': '$x'. " +
              s"Name must contain digits, lowercase letters or underscore. First symbol must be a letter"
          }
        }
    }

    // 'dataProvider' field
    Option(this.dataProvider) match {
      case None =>
        errors += "'Data-provider' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += "'Data-provider' is required"
        } else {
          val dataProviderObj = providerDAO.get(x)
          val allowedTypes = List(ProviderLiterals.cassandraType, ProviderLiterals.aerospikeType)
          dataProviderObj match {
            case None =>
              errors += s"Data-provider '$x' does not exist"
            case Some(provider) =>
              if (!allowedTypes.contains(provider.providerType)) {
                errors += s"'Data-provider' must be one of type: ${allowedTypes.mkString("[", ", ", "]")} " +
                  s"('${provider.providerType}' is given instead)"
              }
          }
        }
    }

    // 'dataNamespace' field
    Option(this.dataNamespace) match {
      case None =>
        errors += "'Data-namespace' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Data-namespace' is required"
        }
        else {
          if (!validateNamespace(x)) {
            errors += s"Service has incorrect 'data-namespace': '$x'. " +
              s"Name must contain digits, lowercase letters or underscore. First symbol must be a letter"
          }
        }
    }

    Option(this.lockProvider) match {
      case None =>
        errors += s"'Lock-provider' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += "'Lock-provider' is required"
        } else {
          val lockProviderObj = providerDAO.get(x)
          lockProviderObj match {
            case None =>
              errors += s"Lock-provider '$x' does not exist"
            case Some(provider) =>
              if (provider.providerType != ProviderLiterals.zookeeperType) {
                errors += s"'Lock-provider' must be of type '${ProviderLiterals.zookeeperType}' " +
                  s"('${provider.providerType}' is given instead)"
              }
          }
        }
    }

    // 'lockNamespace' field
    Option(this.lockNamespace) match {
      case None =>
        errors += "'Lock-namespace' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Lock-namespace' is required"
        }
        else {
          if (!validateNamespace(x)) {
            errors += s"Service has incorrect 'lock-namespace': '$x'. " +
              s"Name must contain digits, lowercase letters or underscore. First symbol must be a letter"
          }
        }
    }

    errors
  }
}
