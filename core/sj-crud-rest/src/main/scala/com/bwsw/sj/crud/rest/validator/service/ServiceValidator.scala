package com.bwsw.sj.crud.rest.validator.service

import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.{AerospikeClient, Host, Info}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceConstants
import com.bwsw.sj.common.rest.entities.service._
import com.bwsw.sj.crud.rest.utils.ValidationUtils
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object ServiceValidator extends ValidationUtils {

  import ServiceConstants._

  private val logger = LoggerFactory.getLogger(getClass.getName)

  lazy val serviceDAO = ConnectionRepository.getServiceManager
  lazy val providerDAO = ConnectionRepository.getProviderService

  /**
   * Validating input parameters for service and filling-in the service object
   *
   * @param initialData - input parameters for service being validated
   * @param service - service object to fill
   * @return - errors
   */
  def validate(initialData: ServiceData, service: Service) = {
    logger.debug(s"Service ${initialData.name}. Start service validation.")

    val errors = new ArrayBuffer[String]()

    // 'serviceType field
    Option(initialData.serviceType) match {
      case None =>
        errors += s"'Type' is required"
      case Some(x) =>
        if (!serviceTypes.contains(x)) errors += s"Unknown 'type' provided. Must be one of: ${serviceTypes.mkString("[", ", ", "]")}"
    }

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'Name' is required"
      case Some(x) =>
        if (serviceDAO.get(x).isDefined) {
          errors += s"Service with name $x already exists"
        }

        if (!validateName(x)) {
          errors += s"Service has incorrect name: $x. Name of service must be contain digits, lowercase letters or hyphens. First symbol must be letter"
        }
    }

    // serviceType-dependent extra fields
    initialData.serviceType match {
      case "CassDB" =>
        val cassDBServiceData = initialData.asInstanceOf[CassDBServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(cassDBServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'keyspace' field
        errors ++= validateStringFieldRequired(cassDBServiceData.keyspace, "Keyspace")
        errors ++= validateNamespace(cassDBServiceData.keyspace)

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[CassandraService].provider = providerObj.get
          service.asInstanceOf[CassandraService].keyspace = cassDBServiceData.keyspace
        }


      case "ESInd" =>
        val esindServiceData = initialData.asInstanceOf[EsIndServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(esindServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'index', 'login', 'password' fields
        errors ++= validateStringFieldRequired(esindServiceData.index, "Index")
        errors ++= validateNamespace(esindServiceData.index)

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[ESService].provider = providerObj.get
          service.asInstanceOf[ESService].index = esindServiceData.index
        }


      case "KfkQ" =>
        val kfkqServiceData = initialData.asInstanceOf[KfkQServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(kfkqServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'zkProvider' field
        var zkProviderObj: Option[Provider] = None
        Option(kfkqServiceData.zkProvider) match {
          case None =>
            errors += s"'Zk-provider' is required"
          case Some(p) =>
            zkProviderObj = providerDAO.get(p)
            zkProviderObj match {
              case Some(provider) =>
                if (provider.providerType != "zookeeper") {
                  errors += s"'Zk-provider' must be of type 'zookeeper' " +
                    s"('${provider.providerType}' is given instead)"
                }
              case None => errors += s"Zookeeper provider '$p' does not exist"
            }
        }

        // 'zkNamespace' field
        errors ++= validateStringFieldRequired(kfkqServiceData.zkNamespace, "ZK-namespace")
        errors ++= validateNamespace(kfkqServiceData.zkNamespace)

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[KafkaService].provider = providerObj.get
          service.asInstanceOf[KafkaService].zkProvider = zkProviderObj.get
          service.asInstanceOf[KafkaService].zkNamespace = kfkqServiceData.zkNamespace
        }

      case "TstrQ" =>
        val tstrqServiceData = initialData.asInstanceOf[TstrQServiceData]

        var metadataProviderObj, dataProviderObj, lockProviderObj: Option[Provider] = None

        // 'metadataProvider' field
        Option(tstrqServiceData.metadataProvider) match {
          case None =>
            errors += s"'Metadata-provider' is required"
          case Some(x) =>
            metadataProviderObj = providerDAO.get(x)
            metadataProviderObj match {
              case Some(provider) =>
                if (provider.providerType != "cassandra") {
                  errors += s"'Metadata-provider' must be of type 'cassandra' " +
                    s"('${provider.providerType}' is given instead)"
                }
              case None => errors += s"Metadata-provider '$x' does not exist"

            }
        }

        // 'metadataNamespace' field
        errors ++= validateStringFieldRequired(tstrqServiceData.metadataNamespace, "Metadata-namespace")
        errors ++= validateNamespace(tstrqServiceData.metadataNamespace)

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
        errors ++= validateStringFieldRequired(tstrqServiceData.dataNamespace, "Data-namespace")
        errors ++= validateNamespace(tstrqServiceData.dataNamespace)

        Option(tstrqServiceData.lockProvider) match {
          case None =>
            errors += s"'Lock-provider' is required"
          case Some(x) =>
            lockProviderObj = providerDAO.get(x)
            lockProviderObj match {
              case Some(provider) =>
                if (provider.providerType != "zookeeper") {
                  errors += s"'Lock-provider' must be of type 'zookeeper' " +
                    s"('${provider.providerType}' is given instead)"
                }
              case None =>
                errors += s"Lock-provider '$x' does not exist"
            }
        }

        // 'lockNamespace' field
        errors ++= validateStringFieldRequired(tstrqServiceData.lockNamespace, "Lock-namespace")
        errors ++= validateNamespace(tstrqServiceData.lockNamespace)

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[TStreamService].metadataProvider = metadataProviderObj.get
          service.asInstanceOf[TStreamService].metadataNamespace = tstrqServiceData.metadataNamespace
          service.asInstanceOf[TStreamService].dataProvider = dataProviderObj.get
          service.asInstanceOf[TStreamService].dataNamespace = tstrqServiceData.dataNamespace
          service.asInstanceOf[TStreamService].lockProvider = lockProviderObj.get
          service.asInstanceOf[TStreamService].lockNamespace = tstrqServiceData.lockNamespace
        }

      case "ZKCoord" =>
        val zkcoordServiceData = initialData.asInstanceOf[ZKCoordServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(zkcoordServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'namespace' field
        errors ++= validateStringFieldRequired(zkcoordServiceData.namespace, "Namespace")
        errors ++= validateNamespace(zkcoordServiceData.namespace)

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[ZKService].provider = providerObj.get
          service.asInstanceOf[ZKService].namespace = zkcoordServiceData.namespace
        }

      case "RDSCoord" =>
        val rdscoordServiceData = initialData.asInstanceOf[RDSCoordServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(rdscoordServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'namespace' field
        errors ++= validateStringFieldRequired(rdscoordServiceData.namespace, "Namespace")
        errors ++= validateNamespace(rdscoordServiceData.namespace)

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[RedisService].provider = providerObj.get
          service.asInstanceOf[RedisService].namespace = rdscoordServiceData.namespace
        }

      case "ArspkDB" =>
        val arspkServiceData = initialData.asInstanceOf[ArspkDBServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(arspkServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'namespace' field
        errors ++= validateStringFieldRequired(arspkServiceData.namespace, "Namespace")
        errors ++= validateNamespace(arspkServiceData.namespace)

        if (errors.isEmpty) {
          val provider = providerObj.get
          val policy = new ClientPolicy()
          val hosts = provider.hosts.map(x => {
            val address = x.split(":")
            new Host(address(0), address(1).toInt)
          })

          val client = new AerospikeClient(policy, hosts: _*)
          val node = client.getNodes()(0)
          val namespaces = Info.request(null, node, "namespaces").split(";")
          if (!namespaces.contains(arspkServiceData.namespace)) {
            errors += s"Aerospike namespace: '${arspkServiceData.namespace}' does not exist. " +
              s"Create it and please try again"
          }
        }

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[AerospikeService].provider = providerObj.get
          service.asInstanceOf[AerospikeService].namespace = arspkServiceData.namespace
        }

      case "JDBC" =>
        val jdbcServiceData = initialData.asInstanceOf[JDBCServiceData]

        // 'provider' field
        val (providerErrors, providerObj) = validateProvider(jdbcServiceData.provider, initialData.serviceType)
        errors ++= providerErrors

        // 'namespace', 'login', 'password' fields
        errors ++= validateStringFieldRequired(jdbcServiceData.namespace, "Namespace")
        errors ++= validateNamespace(jdbcServiceData.namespace)

        // filling-in service object serviceType-dependent extra fields
        if (errors.isEmpty) {
          service.asInstanceOf[JDBCService].provider = providerObj.get
          service.asInstanceOf[JDBCService].namespace = jdbcServiceData.namespace
          service.asInstanceOf[JDBCService].login = jdbcServiceData.login
          service.asInstanceOf[JDBCService].password = jdbcServiceData.password
        }
    }

    // filling-in service object common fields
    if (errors.isEmpty) {
      service.serviceType = initialData.serviceType
      service.name = initialData.name
      service.description = initialData.description
    }

    def validateStringFieldRequired(fieldData: String, fieldJsonName: String) = {
      val errors = new ArrayBuffer[String]()
      Option(fieldData) match {
        case None =>
          errors += s"'$fieldJsonName' is required"
        case Some(x) =>
      }
      errors
    }

    def validateNamespace(namespace: String) = {
      val errors = new ArrayBuffer[String]()

      if (!validateServiceNamespace(namespace)) {
        errors += s"Service has incorrect parameter: $namespace. " +
          s"Name must be contain digits, lowercase letters or underscore. First symbol must be a letter"
      }

      errors
    }

    def validateProvider(provider: String, serviceType: String) = {
      val providerErrors = new ArrayBuffer[String]()
      var providerObj: Option[Provider] = None
      serviceType match {
        case _ if serviceTypes.contains(serviceType) =>
          Option(provider) match {
            case None =>
              providerErrors += s"'Provider' is required"
            case Some(p) =>
              providerObj = providerDAO.get(p)
              if (providerObj.isEmpty) {
                providerErrors += s"Provider '$p' does not exist"
              } else if (providerObj.get.providerType != serviceTypeProviders(serviceType)) {
                providerErrors += s"Provider for '$serviceType' service must be of type '${serviceTypeProviders(serviceType)}' " +
                  s"('${providerObj.get.providerType}' is given instead)"
              }
          }
      }
      (providerErrors, providerObj)
    }

    errors
  }
}
