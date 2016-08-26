package com.bwsw.sj.crud.rest.validator.module

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.{TStreamSjStream, TStreamService}
import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask, InstanceStage, Instance}
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.common.StreamConstants._
import com.bwsw.sj.crud.rest.entities.module.{InputInstanceMetadata, ModuleSpecification, InstanceMetadata}
import com.bwsw.sj.crud.rest.utils.ConvertUtil._
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Validator for input-streaming instance
 *
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
  def createInstance(parameters: InputInstanceMetadata) = {
    logger.debug(s"Instance ${parameters.name}. Create model object.")
    val instance = convertToModelInstance(parameters)
    val stages = scala.collection.mutable.Map[String, InstanceStage]()
    parameters.outputs.foreach { stream =>
      val instanceStartTask = new InstanceStage
      instanceStartTask.state = toHandle
      instanceStartTask.datetime = Calendar.getInstance().getTime
      instanceStartTask.duration = 0
      stages.put(stream, instanceStartTask)
    }
    createTasks(instance.asInstanceOf[InputInstance])
    val instanceTask = new InstanceStage
    instanceTask.state = toHandle
    instanceTask.datetime = Calendar.getInstance().getTime
    instanceTask.duration = 0
    stages.put(instance.name, instanceTask)
    instance.stages = mapAsJavaMap(stages)
    Some(instance)
  }

  /**
   * Create tasks object for instance of input module
   *
   * @param instance - instance for input module
   */
  def createTasks(instance: InputInstance): Unit = {
    logger.debug(s"Instance ${instance.name}. Create tasks for input instance.")

    val tasks = mutable.Map[String, InputTask]()

    for (i <- 0 until instance.parallelism) {
      val task = new InputTask("", 0)
      tasks.put(s"${instance.name}-task$i", task)
    }
    instance.tasks = mapAsJavaMap(tasks)
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
                                     errors: ArrayBuffer[String]): (ArrayBuffer[String], Option[Instance]) = {
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

    var validatedInstance: Option[Instance] = None
    if (outputStreams.nonEmpty) {
      val allStreams = outputStreams
      val inputInstanceMetadata = parameters.asInstanceOf[InputInstanceMetadata]

      val service = allStreams.head.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'."
      } else {
        checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStreamType)).map(_.asInstanceOf[TStreamSjStream]))
      }

      parameters.parallelism = checkParallelism(parameters.parallelism, errors)
      if (parameters.parallelism != null)
        checkBackupNumber(inputInstanceMetadata, errors)

      validatedInstance = createInstance(inputInstanceMetadata)
    }
    (errors, validatedInstance)
  }

  private def checkParallelism(parallelism: Any, errors: ArrayBuffer[String]) = {
    parallelism match {
      case dig: Int =>
        dig
      case _ =>
        errors += "Unknown type of 'parallelism' parameter. Must be Int."
        null
    }
  }

  private def checkBackupNumber(parameters: InputInstanceMetadata, errors: ArrayBuffer[String]) = {
    if (parameters.parallelism.asInstanceOf[Int] <= (parameters.backupCount + parameters.asyncBackupCount))
      errors += "Parallelism must be greater than the total number of backups."
  }

  /**
   * Validating input parameters for input-streaming module
   *
   * @param parameters - input parameters for running module
   * @return - List of errors
   */
  override def validate(parameters: InstanceMetadata,
                        specification: ModuleSpecification): (ArrayBuffer[String], Option[Instance]) = {
    logger.debug(s"Instance: ${parameters.name}. Start input-streaming validation.")
    val errors = super.generalOptionsValidate(parameters)

    val instance = parameters.asInstanceOf[InputInstanceMetadata]

    if (instance.lookupHistory > 0) {
      if (instance.evictionPolicy == null) {
        errors += s"Eviction policy attribute must be not null, if 'lookup-history' is greater zero."
      }
    } else if (instance.lookupHistory == 0) {
      if (instance.defaultEvictionPolicy == null) {
        errors += s"Default eviction policy attribute must be not null, if 'lookup-history' is zero."
      }
      if (instance.queueMaxSize == 0) {
        errors += s"Queue max size attribute must be greater than zero, if 'lookup-history' is zero."
      }
    } else {
      errors += s"Lookup history attribute must be greater than zero."
    }

    if (instance.defaultEvictionPolicy != null) {
      val defaultEvictionPolicy = instance.defaultEvictionPolicy
      if (!defaultEvictionPolicies.contains(defaultEvictionPolicy)) {
        errors += s"Unknown value of 'default-eviction-policy' attribute: $defaultEvictionPolicy. " +
          s"Eviction-policy must be 'LRU' or 'LFU'."
      }
    }

    if (instance.evictionPolicy != null) {
      val evictionPolicy = instance.evictionPolicy
      if (!evictionPolicies.contains(evictionPolicy)) {
        errors += s"Unknown value of 'eviction-policy' attribute: $evictionPolicy. " +
          s"Eviction-policy must be 'fix-time' or 'expanded-time'."
      }
    }

    if (instance.backupCount < 0 || instance.backupCount > 6)
      errors += "Backup count must be in the interval from 0 to 6"

    streamOptionsValidate(parameters, specification, errors)
  }
}
