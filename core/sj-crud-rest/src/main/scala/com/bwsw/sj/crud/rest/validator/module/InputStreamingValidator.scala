package com.bwsw.sj.crud.rest.validator.module

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask, Instance, InstanceStage}
import com.bwsw.sj.common.DAL.model.{TStreamService, TStreamSjStream}
import com.bwsw.sj.common.rest.entities.module.{InputInstanceMetadata, InstanceMetadata, ModuleSpecification}
import com.bwsw.sj.common.utils.EngineConstants._
import com.bwsw.sj.common.utils.StreamConstants._
import com.bwsw.sj.crud.rest.utils.ConvertUtil._
import org.slf4j.{Logger, LoggerFactory}

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
   * Validating input parameters for input-streaming module
   *
   * @param parameters - input parameters for running module
   * @return - List of errors
   */
  override def validate(parameters: InstanceMetadata,
                        specification: ModuleSpecification): (ArrayBuffer[String], Option[Instance]) = {
    logger.debug(s"Instance: ${parameters.name}. Start input-streaming validation.")
    val errors = super.validateGeneralOptions(parameters)

    val instance = parameters.asInstanceOf[InputInstanceMetadata]

    if (instance.lookupHistory < 0) {
      errors += s"Lookup history attribute must be greater than zero or equal to zero"
    }

    if (instance.queueMaxSize < 0) {
      errors += s"Queue max size attribute must be greater than zero or equal to zero"
    }

    if (!defaultEvictionPolicies.contains(instance.defaultEvictionPolicy)) {
      errors += s"Unknown value of 'default-eviction-policy' attribute: ${instance.defaultEvictionPolicy}. " +
        s"Eviction-policy must be one of: ${defaultEvictionPolicies.mkString("[", ",", "]")}"
    }

    if (!evictionPolicies.contains(instance.evictionPolicy)) {
      errors += s"Unknown value of 'eviction-policy' attribute: ${instance.evictionPolicy}. " +
        s"Eviction-policy must be one of: ${evictionPolicies.mkString("[", ",", "]")}"
    }

    if (instance.backupCount < 0 || instance.backupCount > 6)
      errors += "Backup count must be in the interval from 0 to 6"

    validateStreamOptions(instance, specification, errors)
  }

  /**
   * Validating options of streams of instance for module
   *
   * @param parameters    - Input instance parameters
   * @param specification - Specification of module
   * @param errors        - List of validating errors
   * @return - List of errors and validating instance (null, if errors non empty)
   */
  def validateStreamOptions(parameters: InputInstanceMetadata,
                            specification: ModuleSpecification,
                            errors: ArrayBuffer[String]): (ArrayBuffer[String], Option[Instance]) = {
    logger.debug(s"Instance: ${parameters.name}. Stream options validation.")
    var validatedInstance: Option[Instance] = None

    // 'outputs' field
    val outputsCardinality = specification.outputs("cardinality").asInstanceOf[Array[Int]]
    if (parameters.outputs.length < outputsCardinality(0)) {
      errors += s"Count of outputs cannot be less than ${outputsCardinality(0)}."
    }
    if (parameters.outputs.length > outputsCardinality(1)) {
      errors += s"Count of outputs cannot be more than ${outputsCardinality(1)}."
    }
    if (doesContainDoubles(parameters.outputs.toList)) {
      errors += s"Outputs contain the non-unique streams"
    }
    val outputStreams = getStreams(parameters.outputs.toList)
    parameters.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += s"Output stream '$streamName' does not exists"
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be one of the following type: ${outputTypes.mkString("[", ",", "]")}"
    }

    if (outputStreams.nonEmpty) {
      val tStreamsServices = getStreamServices(outputStreams)
      if (tStreamsServices.size != 1) {
        errors += s"All t-streams should have the same service"
      } else {
        val service = serviceDAO.get(tStreamsServices.head)
        if (!service.get.isInstanceOf[TStreamService]) {
          errors += s"Service for t-streams must be 'TstrQ'"
        } else {
          checkTStreams(errors, outputStreams.filter(s => s.streamType.equals(tStreamType)).map(_.asInstanceOf[TStreamSjStream]))
        }
      }

      // 'parallelism' field
      Option(parameters.parallelism) match {
        case None =>
          errors += s"'Parallelism' is required"
        case Some(x) =>
          x match {
            case dig: Int =>
              checkBackupNumber(parameters, errors)
            case _ =>
              errors += "Unknown type of 'parallelism' parameter. Must be Int"
          }
      }

      validatedInstance = createInstance(parameters)
    } else {
      errors += "'Outputs' attribute is empty" //todo needs? or outputs can be empty
    }

    (errors, validatedInstance)
  }

  private def checkBackupNumber(parameters: InputInstanceMetadata, errors: ArrayBuffer[String]) = {
    val parallelism = parameters.parallelism.asInstanceOf[Int]
    if (parallelism <= 0) {
      errors += "Parallelism must be greater than 0"
    }
    if (parallelism <= (parameters.backupCount + parameters.asyncBackupCount)) {
      errors += "Parallelism must be greater than the total number of backups"
    }
  }

  /**
   * Create entity of input instance for saving to database
   *
   * @return
   */
  private def createInstance(parameters: InputInstanceMetadata) = {
    logger.debug(s"Instance ${parameters.name}. Create model object.")
    val instance = instanceMetadataToInstance(parameters)
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
}
