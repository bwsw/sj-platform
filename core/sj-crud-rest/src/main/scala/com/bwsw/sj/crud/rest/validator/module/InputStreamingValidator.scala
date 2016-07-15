package com.bwsw.sj.crud.rest.validator.module

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.{TStreamSjStream, TStreamService}
import com.bwsw.sj.common.DAL.model.module.{InstanceStage, Instance}
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.sj.crud.rest.entities.module.{InputInstanceMetadata, ModuleSpecification, InstanceMetadata}
import com.bwsw.sj.crud.rest.utils.ConvertUtil._
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
  * Validator for input-streaming instance
  * Created: 11/07/2016
  *
  * @author Kseniya Tomskikh
  */
class InputStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Create entity of input instance for saving to database
    *
    * @return
    */
  def createInstance(parameters: InputInstanceMetadata): Instance = {
    logger.debug(s"Instance ${parameters.name}. Create model object.")
    val instance = convertToModelInstance(parameters)
    val stages = scala.collection.mutable.Map[String, InstanceStage]()
    val instanceTask = new InstanceStage
    instanceTask.state = toHandle
    instanceTask.datetime = Calendar.getInstance().getTime
    instanceTask.duration = 0
    stages.put(instance.name, instanceTask)
    instance.stages = mapAsJavaMap(stages)
    instance
  }

  /**
    * Validating options of streams of instance for module
    *
    * @param parameters    - Input instance parameters
    * @param specification - Specification of module
    * @param errors        - List of validating errors
    * @return - List of errors and validating instance (null, if errors non empty)
    */
  override def streamOptionsValidate(parameters: InstanceMetadata,
                                     specification: ModuleSpecification,
                                     errors: ArrayBuffer[String]): (ArrayBuffer[String], Instance) = {
    logger.debug(s"Instance: ${parameters.name}. Stream options validation.")

    if (!parameters.checkpointMode.equals("every-nth")) {
      errors += s"Checkpoint-mode attribute for output-streaming module must be only 'every-nth'."
    }

    if (parameters.inputs != null) {
      errors += s"Unknown attribute 'inputs'."
    }

    val outputsCardinality = specification.outputs("cardinality").asInstanceOf[Array[Int]]
    if (parameters.outputs.length < outputsCardinality(0)) {
      errors += s"Count of outputs cannot be less than ${outputsCardinality(0)}."
    }
    if (parameters.outputs.length > outputsCardinality(1)) {
      errors += s"Count of outputs cannot be more than ${outputsCardinality(1)}."
    }
    if (listHasDoubles(parameters.outputs.toList)) {
      errors += s"Outputs is not unique."
    }
    val outputStreams = getStreams(parameters.outputs.toList)
    parameters.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += s"Output stream '$streamName' is not exists."
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be in: ${outputTypes.mkString(", ")}."
    }

    if (parameters.startFrom != null) {
      errors += s"Unknown attribute 'start-from'."
    }

    var validatedInstance: Instance = null
    if (outputStreams.nonEmpty) {
      val allStreams = outputStreams

      val service = allStreams.head.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'."
      } else {
        checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStream)).map(_.asInstanceOf[TStreamSjStream]))
      }

      validatedInstance = createInstance(parameters.asInstanceOf[InputInstanceMetadata])
    }
    (errors, validatedInstance)
  }

  /**
    * Validating input parameters for input-streaming module
    *
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  override def validate(parameters: InstanceMetadata,
                        specification: ModuleSpecification): (ArrayBuffer[String], Instance) = {
    logger.debug(s"Instance: ${parameters.name}. Start input-streaming validation.")
    val errors = super.generalOptionsValidate(parameters)

    val defaultEvictionPolicy = parameters.asInstanceOf[InputInstanceMetadata].defaultEvictionPolicy
    if (!defaultEvictionPolicies.contains(defaultEvictionPolicy)) {
      errors += s"Unknown value of 'default-eviction-policy' attribute: $defaultEvictionPolicy. " +
        s"Eviction-policy must be 'LRU' or 'LFU'."
    }

    val evictionPolicy = parameters.asInstanceOf[InputInstanceMetadata].evictionPolicy
    if (!evictionPolicies.contains(evictionPolicies)) {
      errors += s"Unknown value of 'eviction-policy' attribute: $evictionPolicy. " +
        s"Eviction-policy must be 'fix-time' or 'expanded-time'."
    }

    streamOptionsValidate(parameters, specification, errors)
  }
}
