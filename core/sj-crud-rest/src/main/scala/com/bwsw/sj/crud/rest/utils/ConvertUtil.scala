package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.rest.entities.config.ConfigurationSettingData
import com.bwsw.sj.common.rest.entities.module._
import com.bwsw.sj.common.rest.entities.provider.ProviderData
import com.bwsw.sj.common.rest.entities.service._
import com.bwsw.sj.common.rest.entities.stream._
import org.slf4j.LoggerFactory

/**
 * Methods for converting protocol entity to model entity
 * and model entity to protocol entity
 *
 *
 * @author Kseniya Tomskikh
 */
object ConvertUtil {

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val serializer = new JsonSerializer

  def instanceToInstanceMetadata(modelInstance: Instance): InstanceMetadata = {
    logger.debug(s"Convert model instance ${modelInstance.name} to protocol instance.")
    modelInstance.toProtocolInstance()
  }

  def instanceMetadataToInstance(protocolInstance: InstanceMetadata): Instance = {
    logger.debug(s"Convert protocol instance ${protocolInstance.name} to model instance.")
    protocolInstance.toModelInstance()
  }

  def specificationToSpecificationData(specification: Specification) = {
    logger.debug(s"Convert model specification ${specification.name} to protocol specification.")
    ModuleSpecification(specification.name,
      specification.description,
      specification.version,
      specification.author,
      specification.license,
      Map("cardinality" -> specification.inputs.cardinality,
        "types" -> specification.inputs.types),
      Map("cardinality" -> specification.outputs.cardinality,
        "types" -> specification.outputs.types),
      specification.moduleType,
      specification.engineName,
      specification.engineVersion,
      serializer.deserialize[Map[String, Any]](specification.options),
      specification.validateClass,
      specification.executorClass)
  }

  def streamToStreamData(stream: SjStream) = {
    logger.debug(s"Convert model stream ${stream.name} to protocol stream.")
    var streamData: SjStreamData = null
    stream match {
      case s: TStreamSjStream =>
        streamData = new TStreamSjStreamData
        val generatorType = stream.asInstanceOf[TStreamSjStream].generator.generatorType
        val generator = new GeneratorData(
          generatorType,
          if (generatorType != "local") stream.asInstanceOf[TStreamSjStream].generator.service.name else null,
          if (generatorType != "local") stream.asInstanceOf[TStreamSjStream].generator.instanceCount else 0
        )
        streamData.asInstanceOf[TStreamSjStreamData].partitions = stream.asInstanceOf[TStreamSjStream].partitions
        streamData.asInstanceOf[TStreamSjStreamData].generator = generator
      case s: KafkaSjStream =>
        streamData = new KafkaSjStreamData
        streamData.asInstanceOf[KafkaSjStreamData].partitions = stream.asInstanceOf[KafkaSjStream].partitions
        streamData.asInstanceOf[KafkaSjStreamData].replicationFactor = stream.asInstanceOf[KafkaSjStream].replicationFactor
      case s: ESSjStream =>
        streamData = new ESSjStreamData
      case s: JDBCSjStream =>
        streamData = new JDBCSjStreamData
    }
    streamData.name = stream.name
    streamData.description = stream.description
    streamData.service = stream.service.name
    streamData.streamType = stream.streamType
    streamData.tags = stream.tags
    streamData
  }

  def serviceToServiceData(service: Service) = {
    logger.debug(s"Convert model service ${service.name} to protocol service.")
    var serviceData: ServiceData = null
    service match {
      case s: CassandraService =>
        serviceData = new CassDBServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[CassDBServiceData].provider = s.provider.name
        serviceData.asInstanceOf[CassDBServiceData].keyspace = s.keyspace
      case s: ESService =>
        serviceData = new EsIndServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[EsIndServiceData].provider = s.provider.name
        serviceData.asInstanceOf[EsIndServiceData].index = s.index
      case s: KafkaService =>
        serviceData = new KfkQServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[KfkQServiceData].provider = s.provider.name
        serviceData.asInstanceOf[KfkQServiceData].zkProvider = s.zkProvider.name
        serviceData.asInstanceOf[KfkQServiceData].zkNamespace = s.zkNamespace
      case s: TStreamService =>
        serviceData = new TstrQServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[TstrQServiceData].metadataProvider = s.metadataProvider.name
        serviceData.asInstanceOf[TstrQServiceData].metadataNamespace = s.metadataNamespace
        serviceData.asInstanceOf[TstrQServiceData].dataProvider = s.dataProvider.name
        serviceData.asInstanceOf[TstrQServiceData].dataNamespace = s.dataNamespace
        serviceData.asInstanceOf[TstrQServiceData].lockProvider = s.lockProvider.name
        serviceData.asInstanceOf[TstrQServiceData].lockNamespace = s.lockNamespace
      case s: ZKService =>
        serviceData = new ZKCoordServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[ZKCoordServiceData].namespace = s.namespace
        serviceData.asInstanceOf[ZKCoordServiceData].provider = s.provider.name
      case s: RedisService =>
        serviceData = new RDSCoordServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[RDSCoordServiceData].namespace = s.namespace
        serviceData.asInstanceOf[RDSCoordServiceData].provider = s.provider.name
      case s: AerospikeService =>
        serviceData = new ArspkDBServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[ArspkDBServiceData].namespace = s.namespace
        serviceData.asInstanceOf[ArspkDBServiceData].provider = s.provider.name
      case s: JDBCService =>
        serviceData = new JDBCServiceData
        serviceData.name = s.name
        serviceData.description = s.description
        serviceData.asInstanceOf[JDBCServiceData].namespace = s.namespace
        serviceData.asInstanceOf[JDBCServiceData].provider = s.provider.name
        serviceData.asInstanceOf[JDBCServiceData].login = s.login
        serviceData.asInstanceOf[JDBCServiceData].password = s.provider.password
      case _ =>
    }
    serviceData
  }

  def providerToProviderData(provider: Provider) = {
    logger.debug(s"Convert model provider ${provider.name} to protocol provider.")
    val providerData = new ProviderData(
      provider.name,
      provider.description,
      provider.login,
      provider.password,
      provider.providerType,
      provider.hosts
    )
    providerData
  }

  def configSettingToConfigSettingData(configElement: ConfigSetting) = {
    logger.debug(s"Convert model config setting ${configElement.name} to protocol config setting.")
    new ConfigurationSettingData(
      configElement.name.replace(configElement.domain + ".", ""),
      configElement.value
    )
  }
}
