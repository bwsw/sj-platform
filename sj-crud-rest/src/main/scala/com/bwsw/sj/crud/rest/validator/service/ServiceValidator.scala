package com.bwsw.sj.crud.rest.validator.service

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.crud.rest.entities._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mendelbaum_nm
  */
class ServiceValidator {
  import com.bwsw.sj.common.ServiceConstants._

  var serviceDAO: GenericMongoService[Service] = null
  var providerDAO: GenericMongoService[Provider] = null

  /**
    * Validating input parameters for service and filling-in the service object
    *
    * @param initialData - input parameters for service being validated
    * @return - (errors, service)
    */
  def validate(initialData: ServiceData, service: Service) = {
    val errors = new ArrayBuffer[String]()
    serviceDAO = ConnectionRepository.getServiceManager
    providerDAO = ConnectionRepository.getProviderService

    // 'serviceType field
    Option(initialData.serviceType) match {
      case None =>
        errors += s"'type' is required"
      case Some(x) =>
        if (!serviceTypes.contains(x)) errors += s"Unknown 'type' provided. Must be one of: $serviceTypes"
    }

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'name' can not be empty"
        } else {
          if (serviceDAO.get(initialData.name) != null) {
            errors += s"Service with name ${initialData.name} already exists"
          }
        }
    }
    // 'description' field
    errors ++= validateStringFieldRequired(initialData.description, "description")

    // serviceType-dependent extra fields
    initialData.serviceType match {
      case "CassDB" =>
        val cassDBServiceData = initialData.asInstanceOf[CassDBServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(cassDBServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'keyspace' field
        errors ++= validateStringFieldRequired(cassDBServiceData.keyspace, "keyspace")

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[CassandraService].provider = providerObj
          service.asInstanceOf[CassandraService].keyspace = cassDBServiceData.keyspace
        }


      case "ESInd" =>
        val esindServiceData = initialData.asInstanceOf[EsIndServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(esindServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'index' field
        errors ++= validateStringFieldRequired(esindServiceData.index, "index")

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[ESService].provider = providerObj
          service.asInstanceOf[ESService].index = esindServiceData.index
        }


      case "KfkQ" =>
        val kfkqServiceData = initialData.asInstanceOf[KfkQServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(kfkqServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[KafkaService].provider = providerObj
        }

      case "TstrQ" =>
        val tstrqServiceData = initialData.asInstanceOf[TstrQServiceData]

        var metadataProviderObj, dataProviderObj, lockProviderObj: Provider = null

        // 'metadataProvider' field
        Option(tstrqServiceData.metadataProvider) match {
          case None =>
            errors += s"'metadata-provider' is required"
          case Some(x) =>
            if (x.isEmpty) {
              errors += s"'metadata-provider' can not be empty"
            } else {
              metadataProviderObj = providerDAO.get(x)
              if (metadataProviderObj == null) {
                errors += s"Metadata-provider '$x' does not exist"
              } else if (metadataProviderObj.providerType != "cassandra") {
                errors += s"metadata-provider must be of type 'cassandra' " +
                  s"('${metadataProviderObj.providerType}' is given instead)"
              }
            }
        }

        // 'metadataNamespace' field
        errors ++= validateStringFieldRequired(tstrqServiceData.metadataNamespace, "metadata-namespace")

        // 'dataProvider' field
        Option(tstrqServiceData.dataProvider) match {
          case None =>
            errors += s"'data-provider' is required"
          case Some(x) =>
            if (x.isEmpty) {
              errors += s"'data-provider' can not be empty"
            } else {
              dataProviderObj = providerDAO.get(x)
              val allowedTypes = List("cassandra", "aerospike")
              if (dataProviderObj == null) {
                errors += s"Data-provider '$x' does not exist"
              } else if (!allowedTypes.contains(dataProviderObj.providerType)) {
                errors += s"Data-provider must be of type ${allowedTypes.mkString("[","|","]")} " +
                  s"('${dataProviderObj.providerType}' is given instead)"
              }
            }
        }

        // 'dataNamespace' field
        errors ++= validateStringFieldRequired(tstrqServiceData.dataNamespace, "data-namespace")

        Option(tstrqServiceData.lockProvider) match {
          case None =>
            errors += s"'lock-provider' is required"
          case Some(x) =>
            if (x.isEmpty) {
              errors += s"'lock-provider' can not be empty"
            } else {
              lockProviderObj = providerDAO.get(x)
              val allowedTypes = List("zookeeper", "redis")
              if (lockProviderObj == null) {
                errors += s"Lock-provider '$x' does not exist"
              } else if (!allowedTypes.contains(lockProviderObj.providerType)) {
                errors += s"Lock-provider must be of type ${allowedTypes.mkString("[","|","]")} " +
                  s"('${lockProviderObj.providerType}' is given instead)"
              }
            }
        }

        // 'lockNamespace' field
        errors ++= validateStringFieldRequired(tstrqServiceData.lockNamespace, "lock-namespace")

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[TStreamService].metadataProvider = metadataProviderObj
          service.asInstanceOf[TStreamService].metadataNamespace = tstrqServiceData.metadataNamespace
          service.asInstanceOf[TStreamService].dataProvider = dataProviderObj
          service.asInstanceOf[TStreamService].dataNamespace = tstrqServiceData.dataNamespace
          service.asInstanceOf[TStreamService].lockProvider = lockProviderObj
          service.asInstanceOf[TStreamService].lockNamespace = tstrqServiceData.lockNamespace
        }

      case "ZKCoord" =>
        val zkcoordServiceData = initialData.asInstanceOf[ZKCoordServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(zkcoordServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'namespace' field
        errors ++= validateStringFieldRequired(zkcoordServiceData.namespace, "namespace")

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[ZKService].provider = providerObj
          service.asInstanceOf[ZKService].namespace = zkcoordServiceData.namespace
        }

      case "RDSCoord" =>
        val rdscoordServiceData = initialData.asInstanceOf[RDSCoordServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(rdscoordServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'namespace' field
        errors ++= validateStringFieldRequired(rdscoordServiceData.namespace, "namespace")

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[RedisService].provider = providerObj
          service.asInstanceOf[RedisService].namespace = rdscoordServiceData.namespace
        }

      case "ArspkDB" =>
        val arspkServiceData = initialData.asInstanceOf[ArspkDBServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(arspkServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'namespace' field
        errors ++= validateStringFieldRequired(arspkServiceData.namespace, "namespace")

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[AerospikeService].provider = providerObj
          service.asInstanceOf[AerospikeService].namespace = arspkServiceData.namespace
        }
    }

    // filling-in service object common fields
    if (errors.isEmpty) {
      service.serviceType = initialData.serviceType
      service.name = initialData.name
      service.description = initialData.description
    }

    /**
      * Simple validation for required string field
      *
      * @param fieldData - data from field to validate
      * @param fieldJsonName - field name in json
      * @return - errors list
      */
    def validateStringFieldRequired(fieldData: String, fieldJsonName: String) = {
      val errors = new ArrayBuffer[String]()
      Option(fieldData) match {
        case None =>
          errors += s"'$fieldJsonName' is required"
        case Some(x) =>
          if (x.isEmpty)
            errors += s"'$fieldJsonName' can not be empty"
      }
      errors
    }

    /**
      * Simple validation for required string field
      *
      * @param provider - data from field to validate
      * @param serviceType - field name in json
      * @return - (provider errors, loaded provider object) pair
      */
    def validateProvider(provider: String, serviceType: String) = {
      val providerErrors = new ArrayBuffer[String]()
      var providerObj: Provider = null
      serviceType match {
        case _ if serviceTypesWithProvider.contains(serviceType) =>
          Option(provider) match {
            case None =>
              providerErrors += s"'provider' is required for '$serviceType' service"
            case Some(p) =>
              if (p.isEmpty) {
                providerErrors += s"'provider' can not be empty"
              }
              else {
                providerObj = providerDAO.get(p)
                if (providerObj == null) {
                  providerErrors += s"Provider '$p' does not exist"
                } else if (providerObj.providerType != serviceTypeProviders(serviceType)) {
                  providerErrors += s"Provider for '$serviceType' service must be of type '${serviceTypeProviders(serviceType)}' " +
                    s"('${providerObj.providerType}' is given instead)"
                }
              }
          }
      }
      (providerErrors, providerObj)
    }

    errors
  }
}
