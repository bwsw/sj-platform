package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateName
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.ServiceLiterals.{typeToProviderType, types}
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class Service(val serviceType: String,
              val name: String,
              val provider: String,
              val description: String)
             (implicit injector: Injector) {

  protected val connectionRepository = inject[ConnectionRepository]
  private val serviceRepository = connectionRepository.getServiceRepository
  private val providerRepository = connectionRepository.getProviderRepository

  def to(): ServiceDomain = ???

  /**
    * Validates service
    *
    * @return empty array if service is correct, validation errors otherwise
    */
  def validate(): ArrayBuffer[String] = validateGeneralFields()

  /**
    * Validates fields which common for all types of service
    *
    * @return empty array if fields is correct, validation errors otherwise
    */
  protected def validateGeneralFields(): ArrayBuffer[String] = {

    val errors = new ArrayBuffer[String]()

    // 'serviceType field
    Option(this.serviceType) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Type")
        }
        else {
          if (!types.contains(x))
            errors += createMessage("entity.error.unknown.type.must.one.of", x, "service", types.mkString("[", ", ", "]"))
        }
    }

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Service", x, "service")
          }

          if (serviceRepository.get(x).isDefined) {
            errors += createMessage("entity.error.already.exists", "Service", x)
          }
        }
    }

    errors
  }

  /**
    * Checks that provider exists and type of service corresponds to provider
    *
    * @return empty array if validation passed, collection of errors otherwise
    */
  protected def validateProvider(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    Option(this.provider) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Provider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Provider")
        }
        else {
          val providerObj = providerRepository.get(x)
          if (providerObj.isEmpty) {
            errors += createMessage("entity.error.doesnot.exist", "Provider", x)
          } else if (providerObj.get.providerType != typeToProviderType(this.serviceType)) {
            errors += createMessage("entity.error.must.one.type.other.given", "Provider", typeToProviderType(this.serviceType), providerObj.get.providerType)
          }
        }
    }

    errors
  }
}

object Service {

  import scala.collection.JavaConverters._

  def from(serviceDomain: ServiceDomain)(implicit injector: Injector): Service = {
    serviceDomain.serviceType match {
      case ServiceLiterals.aerospikeType =>
        val aerospikeService = serviceDomain.asInstanceOf[AerospikeServiceDomain]

        new AerospikeService(
          name = aerospikeService.name,
          namespace = aerospikeService.namespace,
          provider = aerospikeService.provider.name,
          description = aerospikeService.description,
          serviceType = aerospikeService.serviceType
        )

      case ServiceLiterals.cassandraType =>
        val cassandraService = serviceDomain.asInstanceOf[CassandraServiceDomain]

        new CassandraService(
          name = cassandraService.name,
          keyspace = cassandraService.keyspace,
          provider = cassandraService.provider.name,
          description = cassandraService.description,
          serviceType = cassandraService.serviceType
        )

      case ServiceLiterals.elasticsearchType =>
        val esService = serviceDomain.asInstanceOf[ESServiceDomain]

        new ESService(
          name = esService.name,
          index = esService.index,
          provider = esService.provider.name,
          description = esService.description,
          serviceType = esService.serviceType
        )

      case ServiceLiterals.jdbcType =>
        val jdbcService = serviceDomain.asInstanceOf[JDBCServiceDomain]

        new JDBCService(
          name = jdbcService.name,
          database = jdbcService.database,
          provider = jdbcService.provider.name,
          description = jdbcService.description,
          serviceType = jdbcService.serviceType
        )

      case ServiceLiterals.kafkaType =>
        val kafkaService = serviceDomain.asInstanceOf[KafkaServiceDomain]

        new KafkaService(
          name = kafkaService.name,
          zkProvider = kafkaService.zkProvider.name,
          zkNamespace = kafkaService.zkNamespace,
          provider = kafkaService.provider.name,
          description = kafkaService.description,
          serviceType = kafkaService.serviceType
        )

      case ServiceLiterals.restType =>
        val restService = serviceDomain.asInstanceOf[RestServiceDomain]

        new RestService(
          name = restService.name,
          basePath = restService.basePath,
          httpVersion = RestLiterals.httpVersionToString(restService.httpVersion),
          headers = Map(restService.headers.asScala.toList: _*),
          provider = restService.provider.name,
          description = restService.description,
          serviceType = restService.serviceType
        )

      case ServiceLiterals.tstreamsType =>
        val tStreamService = serviceDomain.asInstanceOf[TStreamServiceDomain]

        new TStreamService(
          name = tStreamService.name,
          prefix = tStreamService.prefix,
          token = tStreamService.token,
          provider = tStreamService.provider.name,
          description = tStreamService.description,
          serviceType = tStreamService.serviceType
        )

      case ServiceLiterals.zookeeperType =>
        val zkService = serviceDomain.asInstanceOf[ZKServiceDomain]

        new ZKService(
          name = zkService.name,
          namespace = zkService.namespace,
          provider = zkService.provider.name,
          description = zkService.description,
          serviceType = zkService.serviceType
        )
    }
  }
}
