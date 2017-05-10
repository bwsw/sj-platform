package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateName
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.ServiceLiterals.types
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}

import scala.collection.mutable.ArrayBuffer

abstract class Service(val serviceType: String,
                       val name: String,
                       val description: String) {

  def asService(): ServiceDomain

  def validate(): ArrayBuffer[String] = validateGeneralFields()

  protected def validateGeneralFields(): ArrayBuffer[String] = {
    val serviceRepository = ConnectionRepository.getServiceRepository
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
}

object Service {

  import scala.collection.JavaConverters._

  def fromService(service: ServiceDomain): Service = {
    service.serviceType match {
      case ServiceLiterals.aerospikeType =>
        val aerospikeService = service.asInstanceOf[AerospikeServiceDomain]

        new AerospikeService(
          name = aerospikeService.name,
          namespace = aerospikeService.namespace,
          provider = aerospikeService.provider.name,
          description = aerospikeService.description,
          serviceType = aerospikeService.serviceType
        )

      case ServiceLiterals.cassandraType =>
        val cassandraService = service.asInstanceOf[CassandraServiceDomain]

        new CassandraService(
          name = cassandraService.name,
          keyspace = cassandraService.keyspace,
          provider = cassandraService.provider.name,
          description = cassandraService.description,
          serviceType = cassandraService.serviceType
        )

      case ServiceLiterals.elasticsearchType =>
        val esService = service.asInstanceOf[ESServiceDomain]

        new ESService(
          name = esService.name,
          index = esService.index,
          provider = esService.provider.name,
          description = esService.description,
          serviceType = esService.serviceType
        )

      case ServiceLiterals.jdbcType =>
        val jdbcService = service.asInstanceOf[JDBCServiceDomain]

        new JDBCService(
          name = jdbcService.name,
          database = jdbcService.database,
          provider = jdbcService.provider.name,
          description = jdbcService.description,
          serviceType = jdbcService.serviceType
        )

      case ServiceLiterals.kafkaType =>
        val kafkaService = service.asInstanceOf[KafkaServiceDomain]

        new KafkaService(
          name = kafkaService.name,
          zkProvider = kafkaService.zkProvider.name,
          zkNamespace = kafkaService.zkNamespace,
          provider = kafkaService.provider.name,
          description = kafkaService.description,
          serviceType = kafkaService.serviceType
        )

      case ServiceLiterals.restType =>
        val restService = service.asInstanceOf[RestServiceDomain]

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
        val tStreamService = service.asInstanceOf[TStreamServiceDomain]

        new TStreamService(
          name = tStreamService.name,
          prefix = tStreamService.prefix,
          token = tStreamService.token,
          provider = tStreamService.provider.name,
          description = tStreamService.description,
          serviceType = tStreamService.serviceType
        )

      case ServiceLiterals.zookeeperType =>
        val zkService = service.asInstanceOf[ZKServiceDomain]

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















