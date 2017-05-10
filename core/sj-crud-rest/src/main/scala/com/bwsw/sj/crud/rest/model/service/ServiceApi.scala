package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service._
import com.bwsw.sj.common.utils.ServiceLiterals
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ServiceApi], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[CassDBServiceApi], name = ServiceLiterals.cassandraType),
  new Type(value = classOf[EsServiceApi], name = ServiceLiterals.elasticsearchType),
  new Type(value = classOf[KfkQServiceApi], name = ServiceLiterals.kafkaType),
  new Type(value = classOf[TstrQServiceApi], name = ServiceLiterals.tstreamsType),
  new Type(value = classOf[ZKCoordServiceApi], name = ServiceLiterals.zookeeperType),
  new Type(value = classOf[ArspkDBServiceApi], name = ServiceLiterals.aerospikeType),
  new Type(value = classOf[JDBCServiceApi], name = ServiceLiterals.jdbcType),
  new Type(value = classOf[RestServiceApi], name = ServiceLiterals.restType)
))
class ServiceApi(@JsonProperty("type") val serviceType: String,
                 val name: String,
                 val description: String = "No description") {

  @JsonIgnore
  def asService(): Service = ???
}

object ServiceApi {

  def fromService(service: Service): ServiceApi = {
    service.serviceType match {
      case ServiceLiterals.aerospikeType =>
        val aerospikeService = service.asInstanceOf[AerospikeService]

        new ArspkDBServiceApi(
          name = aerospikeService.name,
          namespace = aerospikeService.namespace,
          provider = aerospikeService.provider,
          description = aerospikeService.description
        )

      case ServiceLiterals.cassandraType =>
        val cassandraService = service.asInstanceOf[CassandraService]

        new CassDBServiceApi(
          name = cassandraService.name,
          keyspace = cassandraService.keyspace,
          provider = cassandraService.provider,
          description = cassandraService.description
        )

      case ServiceLiterals.elasticsearchType =>
        val esService = service.asInstanceOf[ESService]

        new EsServiceApi(
          name = esService.name,
          index = esService.index,
          provider = esService.provider,
          description = esService.description
        )

      case ServiceLiterals.jdbcType =>
        val jdbcService = service.asInstanceOf[JDBCService]

        new JDBCServiceApi(
          name = jdbcService.name,
          database = jdbcService.database,
          provider = jdbcService.provider,
          description = jdbcService.description
        )

      case ServiceLiterals.kafkaType =>
        val kafkaService = service.asInstanceOf[KafkaService]

        new KfkQServiceApi(
          name = kafkaService.name,
          zkProvider = kafkaService.zkProvider,
          zkNamespace = kafkaService.zkNamespace,
          provider = kafkaService.provider,
          description = kafkaService.description
        )

      case ServiceLiterals.restType =>
        val restService = service.asInstanceOf[RestService]

        new RestServiceApi(
          name = restService.name,
          basePath = restService.basePath,
          httpVersion = restService.httpVersion,
          headers = restService.headers,
          provider = restService.provider,
          description = restService.description
        )

      case ServiceLiterals.tstreamsType =>
        val tStreamService = service.asInstanceOf[TStreamService]

        new TstrQServiceApi(
          name = tStreamService.name,
          prefix = tStreamService.prefix,
          token = tStreamService.token,
          provider = tStreamService.provider,
          description = tStreamService.description
        )

      case ServiceLiterals.zookeeperType =>
        val zkService = service.asInstanceOf[ZKService]

        new ZKCoordServiceApi(
          name = zkService.name,
          namespace = zkService.namespace,
          provider = zkService.provider,
          description = zkService.description
        )
    }
  }
}


