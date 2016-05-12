package com.bwsw.sj.crud.rest.utils

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.{WindowedInstance, Specification, RegularInstance}
import com.bwsw.sj.crud.rest.entities.{WindowedInstanceMetadata, ModuleSpecification, InstanceMetadata}

/**
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
  def convertModelInstanceToApiInstance(instance: RegularInstance) = {
    instance match {
      case timeWindowedInstance: WindowedInstance =>
        val apiInstance = instanceToInstanceMetadata(new WindowedInstanceMetadata, instance).asInstanceOf[WindowedInstanceMetadata]
        apiInstance.timeWindowed = timeWindowedInstance.timeWindowed
        apiInstance.windowFullMax = timeWindowedInstance.windowFullMax
        apiInstance
      case _ => instanceToInstanceMetadata(new InstanceMetadata, instance)
    }
  }

  /**
    * Convert model instance object to API instance
    *
    * @param instance - object of model instance
    * @return - API instance object
    */
  def instanceToInstanceMetadata(apiInstance: InstanceMetadata, instance: RegularInstance): InstanceMetadata = {
    val executionPlan = Map(
      "tasks" -> instance.executionPlan.tasks.map(t => t._1 -> Map("inputs" -> t._2.inputs))
    )
    apiInstance.status = instance.status
    apiInstance.name = instance.name
    apiInstance.description = instance.description
    apiInstance.inputs = instance.inputs
    apiInstance.outputs = instance.outputs
    apiInstance.checkpointMode = instance.checkpointMode
    apiInstance.checkpointInterval = instance.checkpointInterval
    apiInstance.stateManagement = instance.stateManagement
    apiInstance.stateFullCheckpoint = instance.stateFullCheckpoint
    apiInstance.parallelism = instance.parallelism
    apiInstance.options = serializer.deserialize[Map[String, Any]](instance.options)
    apiInstance.startFrom = instance.startFrom
    apiInstance.perTaskCores = instance.perTaskCores
    apiInstance.perTaskRam = instance.perTaskRam
    apiInstance.jvmOptions = Map(instance.jvmOptions.asScala.toList: _*)
    apiInstance.attributes = Map(instance.attributes.asScala.toList: _*)
    apiInstance.idle = instance.idle
    apiInstance.executionPlan = executionPlan
    apiInstance.environments = Map(instance.environments.asScala.toList: _*)
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
    * @param modelInstance - dst object of model instance
    * @param apiInstance - api object of instance
    * @return - object of model instance
    */
  def convertToModelInstance(modelInstance: RegularInstance, apiInstance: InstanceMetadata) = {
    modelInstance.name = apiInstance.name
    modelInstance.description = apiInstance.description
    modelInstance.inputs = apiInstance.inputs
    modelInstance.outputs = apiInstance.outputs
    modelInstance.checkpointMode = apiInstance.checkpointMode
    modelInstance.checkpointInterval = apiInstance.checkpointInterval
    modelInstance.stateFullCheckpoint = apiInstance.stateFullCheckpoint
    modelInstance.stateManagement = apiInstance.stateManagement
    modelInstance.parallelism = apiInstance.parallelism.asInstanceOf[Int]
    modelInstance.options = serializer.serialize(apiInstance.options)
    modelInstance.startFrom = apiInstance.startFrom
    modelInstance.perTaskCores = apiInstance.perTaskCores
    modelInstance.perTaskRam = apiInstance.perTaskRam
    modelInstance.jvmOptions = mapAsJavaMap(apiInstance.jvmOptions)
    modelInstance.attributes = mapAsJavaMap(apiInstance.attributes)
    modelInstance.idle = apiInstance.idle
    modelInstance.environments = mapAsJavaMap(apiInstance.environments)
    modelInstance
  }

}
