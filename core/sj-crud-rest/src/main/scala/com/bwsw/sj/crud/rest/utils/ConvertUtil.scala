package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.entities.config.ConfigurationSettingData
import com.bwsw.sj.crud.rest.entities.module._
import com.bwsw.sj.crud.rest.entities.provider.ProviderData
import com.bwsw.sj.crud.rest.entities.service._
import com.bwsw.sj.crud.rest.entities.stream._
import org.slf4j.LoggerFactory

/**
 * Methods for converting protocol entity to model entity
 * and model entity to protocol entity
 *
 *
 * @author Kseniya Tomskikh
 */
object ConvertUtil {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val serializer = new JsonSerializer

  def instanceToInstanceMetadata(instance: Instance): InstanceMetadata = {
    logger.debug(s"Convert model instance ${instance.name} to protocol instance.")
    instance match {
      case timeWindowedInstance: WindowedInstance =>
        val protocolInstance = instanceToInstanceMetadata(new WindowedInstanceMetadata, instance).asInstanceOf[WindowedInstanceMetadata]
        protocolInstance.timeWindowed = timeWindowedInstance.timeWindowed
        protocolInstance.windowFullMax = timeWindowedInstance.windowFullMax
        protocolInstance.stateManagement = timeWindowedInstance.stateManagement
        protocolInstance.stateFullCheckpoint = timeWindowedInstance.stateFullCheckpoint
        protocolInstance.eventWaitTime = timeWindowedInstance.eventWaitTime
        protocolInstance.inputs = timeWindowedInstance.inputs
        protocolInstance.outputs = timeWindowedInstance.outputs
        protocolInstance.startFrom = timeWindowedInstance.startFrom
        protocolInstance
      case regularInstance: RegularInstance =>
        val protocolInstance = instanceToInstanceMetadata(new RegularInstanceMetadata, instance).asInstanceOf[RegularInstanceMetadata]
        protocolInstance.stateManagement = regularInstance.stateManagement
        protocolInstance.stateFullCheckpoint = regularInstance.stateFullCheckpoint
        protocolInstance.eventWaitTime = regularInstance.eventWaitTime
        protocolInstance.inputs = regularInstance.inputs
        protocolInstance.outputs = regularInstance.outputs
        protocolInstance.startFrom = regularInstance.startFrom
        protocolInstance
      case outputInstance: OutputInstance =>
        val protocolInstance = instanceToInstanceMetadata(new OutputInstanceMetadata, instance).asInstanceOf[OutputInstanceMetadata]
        protocolInstance.input = outputInstance.inputs.head
        protocolInstance.output = outputInstance.outputs.head
        protocolInstance.startFrom = outputInstance.startFrom
        protocolInstance
      case inputInstance: InputInstance =>
        val protocolInstance = instanceToInstanceMetadata(new InputInstanceMetadata, instance).asInstanceOf[InputInstanceMetadata]
        protocolInstance.outputs = inputInstance.outputs
        protocolInstance.defaultEvictionPolicy = inputInstance.defaultEvictionPolicy
        protocolInstance.evictionPolicy = inputInstance.evictionPolicy
        protocolInstance.lookupHistory = inputInstance.lookupHistory
        protocolInstance.queueMaxSize = inputInstance.queueMaxSize
        if (inputInstance.tasks != null) {
          protocolInstance.tasks = Map(inputInstance.tasks.asScala.toList: _*)
        }
        protocolInstance
      case _ => instanceToInstanceMetadata(new InstanceMetadata, instance)
    }
  }
  
  private def instanceToInstanceMetadata(protocolInstance: InstanceMetadata, instance: Instance): InstanceMetadata = {
    if (instance.inputs != null) {
      val executionPlan = Map(
        "tasks" -> instance.executionPlan.tasks.map(t => t._1 -> Map("inputs" -> t._2.inputs))
      )
      protocolInstance.executionPlan = executionPlan
    }
    protocolInstance.status = instance.status
    protocolInstance.name = instance.name
    protocolInstance.description = instance.description
    protocolInstance.checkpointMode = instance.checkpointMode
    protocolInstance.checkpointInterval = instance.checkpointInterval
    protocolInstance.parallelism = instance.parallelism
    protocolInstance.options = serializer.deserialize[Map[String, Any]](instance.options)
    protocolInstance.perTaskCores = instance.perTaskCores
    protocolInstance.performanceReportingInterval = instance.performanceReportingInterval
    protocolInstance.engine = instance.engine
    protocolInstance.perTaskRam = instance.perTaskRam
    protocolInstance.jvmOptions = Map(instance.jvmOptions.asScala.toList: _*)
    if (instance.nodeAttributes != null) {
      protocolInstance.nodeAttributes = Map(instance.nodeAttributes.asScala.toList: _*)
    }
    if (instance.environmentVariables != null) {
      protocolInstance.environmentVariables = Map(instance.environmentVariables.asScala.toList: _*)
    }
    protocolInstance.coordinationService = instance.coordinationService.name
    protocolInstance
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

  def instanceMetadataToInstance(protocolInstance: InstanceMetadata): Instance = {
    logger.debug(s"Convert protocol instance ${protocolInstance.name} to model instance.")
    protocolInstance match {
      case windowedInstanceMetadata: WindowedInstanceMetadata =>
        val modelInstance = instanceMetadataToInstance(new WindowedInstance, windowedInstanceMetadata).asInstanceOf[WindowedInstance]
        modelInstance.timeWindowed = windowedInstanceMetadata.timeWindowed
        modelInstance.windowFullMax = windowedInstanceMetadata.windowFullMax
        modelInstance.stateManagement = windowedInstanceMetadata.stateManagement
        modelInstance.stateFullCheckpoint = windowedInstanceMetadata.stateFullCheckpoint
        modelInstance.eventWaitTime = windowedInstanceMetadata.eventWaitTime
        modelInstance.inputs = windowedInstanceMetadata.inputs
        modelInstance.outputs = windowedInstanceMetadata.outputs
        modelInstance.startFrom = windowedInstanceMetadata.startFrom
        modelInstance
      case regularInstanceMetadata: RegularInstanceMetadata =>
        val modelInstance = instanceMetadataToInstance(new RegularInstance, regularInstanceMetadata).asInstanceOf[RegularInstance]
        modelInstance.stateManagement = regularInstanceMetadata.stateManagement
        modelInstance.stateFullCheckpoint = regularInstanceMetadata.stateFullCheckpoint
        modelInstance.eventWaitTime = regularInstanceMetadata.eventWaitTime
        modelInstance.inputs = regularInstanceMetadata.inputs
        modelInstance.outputs = regularInstanceMetadata.outputs
        modelInstance.startFrom = regularInstanceMetadata.startFrom
        modelInstance
      case outputInstanceMetadata: OutputInstanceMetadata =>
        val modelInstance = instanceMetadataToInstance(new OutputInstance, outputInstanceMetadata).asInstanceOf[OutputInstance]
        modelInstance.inputs = Array(outputInstanceMetadata.input)
        modelInstance.outputs = Array(outputInstanceMetadata.output)
        modelInstance.startFrom = outputInstanceMetadata.startFrom
        modelInstance
      case inputInstanceMetadata: InputInstanceMetadata =>
        val modelInstance = instanceMetadataToInstance(new InputInstance, inputInstanceMetadata).asInstanceOf[InputInstance]
        modelInstance.outputs = inputInstanceMetadata.outputs
        modelInstance.defaultEvictionPolicy = inputInstanceMetadata.defaultEvictionPolicy
        modelInstance.evictionPolicy = inputInstanceMetadata.evictionPolicy
        modelInstance.lookupHistory = inputInstanceMetadata.lookupHistory
        modelInstance.queueMaxSize = inputInstanceMetadata.queueMaxSize
        if (inputInstanceMetadata.tasks != null) {
          modelInstance.tasks = inputInstanceMetadata.tasks
        }
        modelInstance
      case _ => instanceMetadataToInstance(new Instance, protocolInstance)
    }
  }

  private def instanceMetadataToInstance(modelInstance: Instance, apiInstance: InstanceMetadata) = {
    modelInstance.name = apiInstance.name
    modelInstance.description = apiInstance.description
    modelInstance.checkpointMode = apiInstance.checkpointMode
    modelInstance.checkpointInterval = apiInstance.checkpointInterval
    modelInstance.parallelism = apiInstance.parallelism.asInstanceOf[Int]
    modelInstance.options = serializer.serialize(apiInstance.options)
    modelInstance.perTaskCores = apiInstance.perTaskCores
    modelInstance.perTaskRam = apiInstance.perTaskRam
    modelInstance.performanceReportingInterval = apiInstance.performanceReportingInterval
    modelInstance.engine = apiInstance.engine
    modelInstance.jvmOptions = mapAsJavaMap(apiInstance.jvmOptions)
    if (apiInstance.nodeAttributes != null) {
      modelInstance.nodeAttributes = mapAsJavaMap(apiInstance.nodeAttributes)
    }
    if (apiInstance.environmentVariables != null) {
      modelInstance.environmentVariables = mapAsJavaMap(apiInstance.environmentVariables)
    }
    val serviceDAO = ConnectionRepository.getServiceManager
    if (apiInstance.coordinationService != null) {
      val service = serviceDAO.get(apiInstance.coordinationService)
      if (service.isDefined && service.get.isInstanceOf[ZKService]) {
        modelInstance.coordinationService = service.get.asInstanceOf[ZKService]
      }
    }
    modelInstance
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
