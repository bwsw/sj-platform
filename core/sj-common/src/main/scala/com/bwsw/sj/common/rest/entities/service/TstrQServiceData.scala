package com.bwsw.sj.common.rest.entities.service

import com.bwsw.sj.common.DAL.model.TStreamService
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{Service, Provider}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ArrayBuffer

class TstrQServiceData() extends ServiceData() {
  serviceType = Service.tstreamsType
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
        errors += s"'Metadata-provider' is required"
      case Some(x) =>
        val metadataProviderObj = providerDAO.get(x)
        metadataProviderObj match {
          case Some(provider) =>
            if (provider.providerType != Provider.cassandraType) {
              errors += s"'Metadata-provider' must be of type '${Provider.cassandraType}' " +
                s"('${provider.providerType}' is given instead)"
            }
          case None => errors += s"Metadata-provider '$x' does not exist"

        }
    }

    // 'metadataNamespace' field
    errors ++= validateStringFieldRequired(this.metadataNamespace, "Metadata-namespace")
    errors ++= validateNamespace(this.metadataNamespace)

    // 'dataProvider' field
    Option(this.dataProvider) match {
      case None =>
        errors += s"'data-provider' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'data-provider' can not be empty"
        } else {
          val dataProviderObj = providerDAO.get(x)
          val allowedTypes = List(Provider.cassandraType, Provider.aerospikeType)
          dataProviderObj match {
            case Some(provider) =>
              if (!allowedTypes.contains(provider.providerType)) {
                errors += s"Data-provider must be one of type: ${allowedTypes.mkString("[", ", ", "]")} " +
                  s"('${provider.providerType}' is given instead)"
              }
            case None =>
              errors += s"Data-provider '$x' does not exist"
          }
        }
    }

    // 'dataNamespace' field
    errors ++= validateStringFieldRequired(this.dataNamespace, "Data-namespace")
    errors ++= validateNamespace(this.dataNamespace)

    Option(this.lockProvider) match {
      case None =>
        errors += s"'Lock-provider' is required"
      case Some(x) =>
        val lockProviderObj = providerDAO.get(x)
        lockProviderObj match {
          case Some(provider) =>
            if (provider.providerType != Provider.zookeeperType) {
              errors += s"'Lock-provider' must be of type '${Provider.zookeeperType}' " +
                s"('${provider.providerType}' is given instead)"
            }
          case None =>
            errors += s"Lock-provider '$x' does not exist"
        }
    }

    // 'lockNamespace' field
    errors ++= validateStringFieldRequired(this.lockNamespace, "Lock-namespace")
    errors ++= validateNamespace(this.lockNamespace)

    errors
  }
}
