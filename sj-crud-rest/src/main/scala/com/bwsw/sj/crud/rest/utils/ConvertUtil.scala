package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.ZKService
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.entities.module._

/**
  * Methods for converting protocol entity to model entity
  * and model entity to protocol entity
  * Created: 12/05/2016
  *
  * @author Kseniya Tomskikh
  */
object ConvertUtil {
  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  val serializer = new JsonSerializer

  /**
    * Convert model instance to protocol instance for such module type
    *
    * @param instance - Model instance object
    * @return - API instance object for such module type
    */
  def convertModelInstanceToApiInstance(instance: Instance) = {
    instance match {
      case timeWindowedInstance: WindowedInstance =>
        val apiInstance = instanceToInstanceMetadata(new WindowedInstanceMetadata, instance).asInstanceOf[WindowedInstanceMetadata]
        apiInstance.timeWindowed = timeWindowedInstance.timeWindowed
        apiInstance.windowFullMax = timeWindowedInstance.windowFullMax
        apiInstance.stateManagement = timeWindowedInstance.stateManagement
        apiInstance.stateFullCheckpoint = timeWindowedInstance.stateFullCheckpoint
        apiInstance.eventWaitTime = timeWindowedInstance.eventWaitTime
        apiInstance.inputs = timeWindowedInstance.inputs
        apiInstance.outputs = timeWindowedInstance.outputs
        apiInstance
      case regularInstance: RegularInstance =>
        val apiInstance = instanceToInstanceMetadata(new RegularInstanceMetadata, instance).asInstanceOf[RegularInstanceMetadata]
        apiInstance.stateManagement = regularInstance.stateManagement
        apiInstance.stateFullCheckpoint = regularInstance.stateFullCheckpoint
        apiInstance.eventWaitTime = regularInstance.eventWaitTime
        apiInstance.inputs = regularInstance.inputs
        apiInstance.outputs = regularInstance.outputs
        apiInstance
      case outputInstance: OutputInstance =>
        val apiInstance = instanceToInstanceMetadata(new OutputInstanceMetadata, instance).asInstanceOf[OutputInstanceMetadata]
        apiInstance.input = outputInstance.inputs.head
        apiInstance.output = outputInstance.outputs.head
        apiInstance
      case _ => instanceToInstanceMetadata(new InstanceMetadata, instance)
    }
  }

  /**
    * Convert model instance object to API instance
    *
    * @param apiInstance - protocol object of instance
    * @param instance - object of model instance
    * @return - API instance object
    */
  def instanceToInstanceMetadata(apiInstance: InstanceMetadata, instance: Instance): InstanceMetadata = {
    val executionPlan = Map(
      "tasks" -> instance.executionPlan.tasks.map(t => t._1 -> Map("inputs" -> t._2.inputs))
    )
    apiInstance.status = instance.status
    apiInstance.name = instance.name
    apiInstance.description = instance.description
    apiInstance.checkpointMode = instance.checkpointMode
    apiInstance.checkpointInterval = instance.checkpointInterval
    apiInstance.parallelism = instance.parallelism
    apiInstance.options = serializer.deserialize[Map[String, Any]](instance.options)
    apiInstance.startFrom = instance.startFrom
    apiInstance.perTaskCores = instance.perTaskCores
    apiInstance.perTaskRam = instance.perTaskRam
    apiInstance.jvmOptions = Map(instance.jvmOptions.asScala.toList: _*)
    if (instance.nodeAttributes != null) {
      apiInstance.nodeAttributes = Map(instance.nodeAttributes.asScala.toList: _*)
    }
    apiInstance.executionPlan = executionPlan
    if (instance.environmentVariables != null) {
      apiInstance.environmentVariables = Map(instance.environmentVariables.asScala.toList: _*)
    }
    apiInstance.coordinationService = instance.coordinationService.name
    apiInstance
  }

  /**
    * Convert model file specification to protocol file specification
    *
    * @param specification - Model file specification object
    * @return - API file specification object
    */
  def specificationToSpecificationData(specification: Specification) = {
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
      specification.engine,
      serializer.deserialize[Map[String, Any]](specification.options),
      specification.validateClass,
      specification.executorClass)
  }

  /**
    * Convert api instance to db-model instance
    *
    * @param apiInstance - api object of instance
    * @return - object of model instance
    */
  def convertToModelInstance(apiInstance: InstanceMetadata) = {
    apiInstance match {
      case windowedInstanceMetadata: WindowedInstanceMetadata =>
        val modelInstance = instanceMetadataToInstance(new WindowedInstance, windowedInstanceMetadata).asInstanceOf[WindowedInstance]
        modelInstance.timeWindowed = windowedInstanceMetadata.timeWindowed
        modelInstance.windowFullMax = windowedInstanceMetadata.windowFullMax
        modelInstance.stateManagement = windowedInstanceMetadata.stateManagement
        modelInstance.stateFullCheckpoint = windowedInstanceMetadata.stateFullCheckpoint
        modelInstance.eventWaitTime = windowedInstanceMetadata.eventWaitTime
        modelInstance.inputs = windowedInstanceMetadata.inputs
        modelInstance.outputs = windowedInstanceMetadata.outputs
        modelInstance
      case regularInstanceMetadata: RegularInstanceMetadata =>
        val modelInstance = instanceMetadataToInstance(new RegularInstance, regularInstanceMetadata).asInstanceOf[RegularInstance]
        modelInstance.stateManagement = regularInstanceMetadata.stateManagement
        modelInstance.stateFullCheckpoint = regularInstanceMetadata.stateFullCheckpoint
        modelInstance.eventWaitTime = regularInstanceMetadata.eventWaitTime
        modelInstance.inputs = regularInstanceMetadata.inputs
        modelInstance.outputs = regularInstanceMetadata.outputs
        modelInstance
      case outputInstanceMetadata: OutputInstanceMetadata =>
        val modelInstance = instanceMetadataToInstance(new OutputInstance, outputInstanceMetadata).asInstanceOf[OutputInstance]
        modelInstance.inputs = Array(outputInstanceMetadata.input)
        modelInstance.outputs = Array(outputInstanceMetadata.output)
        modelInstance
      case _ => instanceMetadataToInstance(new Instance, apiInstance)
    }
  }

  /**
    * Convert API instance object to model instance
    *
    * @param modelInstance - object of model instance
    * @param apiInstance - protocol object of instance
    * @return - Model instance object
    */
  def instanceMetadataToInstance(modelInstance: Instance, apiInstance: InstanceMetadata) = {
    modelInstance.name = apiInstance.name
    modelInstance.description = apiInstance.description
    modelInstance.checkpointMode = apiInstance.checkpointMode
    modelInstance.checkpointInterval = apiInstance.checkpointInterval
    modelInstance.parallelism = apiInstance.parallelism.asInstanceOf[Int]
    modelInstance.options = serializer.serialize(apiInstance.options)
    modelInstance.startFrom = apiInstance.startFrom
    modelInstance.perTaskCores = apiInstance.perTaskCores
    modelInstance.perTaskRam = apiInstance.perTaskRam
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
      if (service != null && service.isInstanceOf[ZKService]) {
        modelInstance.coordinationService = service.asInstanceOf[ZKService]
      }
    }
    modelInstance
  }

}
