package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.rest.entities.config.ConfigurationSettingData
import com.bwsw.sj.common.rest.entities.module._
import com.bwsw.sj.common.rest.entities.provider.ProviderData
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

  def providerToProviderData(provider: Provider) = {
    logger.debug(s"Convert model provider ${provider.name} to protocol provider.")
    val providerData = new ProviderData(
      provider.name,
      provider.login,
      provider.password,
      provider.providerType,
      provider.hosts,
      provider.description
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
