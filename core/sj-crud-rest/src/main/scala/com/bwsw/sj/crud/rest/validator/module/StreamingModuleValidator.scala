package com.bwsw.sj.crud.rest.validator.module

import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.crud.rest.entities.module.{InstanceMetadata, ModuleSpecification}
import com.bwsw.sj.crud.rest.utils.StreamUtil
import kafka.common.TopicExistsException

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Trait of validator for modules
  * Created: 12/04/2016
  *
  * @author Kseniya Tomskikh
  */
abstract class StreamingModuleValidator {
  import com.bwsw.sj.common.ModuleConstants._
  import com.bwsw.sj.common.StreamConstants._
  import com.bwsw.sj.crud.rest.utils.ConvertUtil._

  val instanceNamePattern = "^[a-zA-Z0-9-]$".r

  var serviceDAO: GenericMongoService[Service] = ConnectionRepository.getServiceManager
  var instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  val serializer: Serializer = new JsonSerializer

  case class InputStream(name: String, mode: String, partitionsCount: Int)

  case class StreamProcess(currentPartition: Int, countFreePartitions: Int)

  /**
    * Create entity of instance for saving to database
    *
    * @param parameters - metadata of instance
    * @param partitionsCount - partitions count of input streams
    * @return
    */
  def createInstance(parameters: InstanceMetadata, partitionsCount: Map[String, Int], streams: Set[SjStream]) = {
    val executionPlan = createExecutionPlan(parameters, partitionsCount)
    val instance = convertToModelInstance(parameters)
    instance.executionPlan = executionPlan
    val stages = scala.collection.mutable.Map[String, InstanceStage]()
    streams.foreach { stream =>
      val instanceStartTask = new InstanceStage
      instanceStartTask.state = toHandle
      instanceStartTask.datetime = Calendar.getInstance().getTime
      instanceStartTask.duration = 0
      stages.put(stream.name, instanceStartTask)
    }
    val instanceTask = new InstanceStage
    instanceTask.state = toHandle
    instanceTask.datetime = Calendar.getInstance().getTime
    instanceTask.duration = 0
    stages.put(instance.name, instanceTask)
    instance.stages = mapAsJavaMap(stages)
    instance
  }


  /**
    * Validating input parameters for streaming module
    *
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  def validate(parameters: InstanceMetadata, specification: ModuleSpecification) = {
    val errors = generalOptionsValidate(parameters)
    streamOptionsValidate(parameters, specification, errors)
  }

  /**
    * Validation base instance options
    *
    * @param parameters - Instance parameters
    * @return - List of errors
    */
  def generalOptionsValidate(parameters: InstanceMetadata) = {
    val errors = new ArrayBuffer[String]()

    if (!parameters.name.matches("""^([a-zA-Z][a-zA-Z0-9-]+)$""")) {
      errors += s"Instance has incorrect name: ${parameters.name}. Name of instance must be contain digits, letters or hyphens. First symbol must be letter."
    }

    val instance = instanceDAO.get(parameters.name)
    if (instance != null) {
      errors += s"Instance for name: ${parameters.name} is exist."
    }

    if (!checkpointModes.contains(parameters.checkpointMode)) {
      errors += s"Unknown value of checkpoint-mode attribute: ${parameters.checkpointMode}."
    }

    if (parameters.options.isEmpty) {
      errors += "Options attribute is empty."
    }

    if (parameters.performanceReportingInterval <= 0) {
      errors += "Performance reporting interval attribute must be greater than zero."
    }

    if (parameters.jvmOptions.isEmpty) {
      errors += "Jvm-options attribute is empty."
    }

    if (parameters.coordinationService.isEmpty) {
      errors += "Coordination service attribute is empty."
    } else {
      val coordService = serviceDAO.get(parameters.coordinationService)
      if (coordService != null) {
        if (!coordService.isInstanceOf[ZKService]) {
          errors += s"Coordination service ${parameters.coordinationService} is not ZKCoord."
        }
      } else {
        errors += s"Coordination service ${parameters.coordinationService} is not exists."
      }
    }

    errors
  }

  /**
    * Validating options of streams of instance for module
    *
    * @param parameters - Input instance parameters
    * @param specification - Specification of module
    * @param errors - List of validating errors
    * @return - List of errors and validating instance (null, if errors non empty)
    */
  def streamOptionsValidate(parameters: InstanceMetadata, specification: ModuleSpecification, errors: ArrayBuffer[String]) = {
    val inputModes = parameters.inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += s"Unknown stream modes. Input streams must have modes 'split' or 'full'."
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if ( parameters.inputs.length < inputsCardinality(0)) {
      errors += s"Count of inputs cannot be less than ${inputsCardinality(0)}."
    }
    if ( parameters.inputs.length > inputsCardinality(1)) {
      errors += s"Count of inputs cannot be more than ${inputsCardinality(1)}."
    }
    if (listHasDoubles(parameters.inputs.toList)) {
      errors += s"Inputs is not unique."
    }
    val inputStreams = getStreams(parameters.inputs.toList.map(_.replaceAll("/split|/full", "")))
    parameters.inputs.toList.map(_.replaceAll("/split|/full", "")).foreach { streamName =>
      if (!inputStreams.exists(s => s.name == streamName)) {
        errors += s"Input stream '$streamName' is not exists."
      }
    }

    val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
    if (inputStreams.exists(s => !inputTypes.contains(s.streamType))) {
      errors += s"Input streams must be in: ${inputTypes.mkString(", ")}.."
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

    if (errors.isEmpty) {
      val allStreams = inputStreams.union(outputStreams)

      val startFrom = parameters.startFrom
      if (!startFromModes.contains(startFrom)) {
        if (allStreams.exists(s => s.streamType.equals(kafka))) {
          errors += s"Start-from attribute must be 'oldest' or 'newest', if instance have kafka-streams."
        } else {
          try {
            startFrom.toLong
          } catch {
            case ex: NumberFormatException =>
              errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
          }
        }
      }

      val tStreamsServices = getStreamServices(allStreams.filter { s =>
        s.streamType.equals(tStream)
      })
      if (tStreamsServices.size != 1) {
        errors += s"All t-streams should have the same service."
      } else {
        val service = serviceDAO.get(tStreamsServices.head)
        if (!service.isInstanceOf[TStreamService]) {
          errors += s"Service for t-streams must be 'TstrQ'."
        } else {
          checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStream)).map(_.asInstanceOf[TStreamSjStream]))
        }
      }

      val kafkaStreams = allStreams.filter(s => s.streamType.equals(kafka)).map(_.asInstanceOf[KafkaSjStream])
      if (kafkaStreams.nonEmpty) {
        if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaService])) {
          errors += s"Service for kafka-streams must be 'KfkQ'."
        } else {
          checkKafkaStreams(errors, kafkaStreams)
        }
      }

      val partitions = getPartitionForStreams(inputStreams)
      val minPartitionCount = if (partitions.nonEmpty) partitions.values.min else 0

      parameters.parallelism = checkParallelism(parameters.parallelism, minPartitionCount, errors)

      val validatedInstance = createInstance(parameters, partitions, allStreams.filter(s => s.streamType.equals(tStream)).toSet)
      (errors, validatedInstance)
    } else {
      (errors, null)
    }

  }

  /**
    * Validating 'parallelism' parameters of instance
    *
    * @param parallelism - Parallelism value
    * @param partitions - Min count of partitions of input streams
    * @param errors - List of errors
    * @return - Validated value of parallelism
    */
  def checkParallelism(parallelism: Any, partitions: Int, errors: ArrayBuffer[String]) = {
    parallelism match {
      case dig: Int =>
        if (dig > partitions) {
          errors += s"Parallelism ($dig) > minimum of partition count ($partitions) in all input stream."
        }
        dig
      case s: String =>
        if (!s.equals("max")) {
          errors += s"Parallelism must be int value or string 'max'."
        }
        partitions
      case _ =>
        errors += "Unknown type of 'parallelism' parameter. Must be Int or String."
        null
    }
  }

  /**
    * Checking and creating t-streams, if streams is not exists
    *
    * @param errors - List of all errors
    * @param allTStreams - all t-streams of instance
    */
  def checkTStreams(errors: ArrayBuffer[String], allTStreams: mutable.Buffer[TStreamSjStream]) = {
    allTStreams.foreach { (stream: TStreamSjStream) =>
      if (errors.isEmpty) {
        val streamCheckResult = StreamUtil.checkAndCreateTStream(stream)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      }
    }
  }

  /**
    * Checking and creating kafka topics, if it's not exists
    *
    * @param errors - list of all errors
    * @param allKafkaStreams - all kafka streams of instance
    */
  def checkKafkaStreams(errors: ArrayBuffer[String], allKafkaStreams: mutable.Buffer[KafkaSjStream]) = {
    allKafkaStreams.foreach { (stream: KafkaSjStream) =>
      if (errors.isEmpty) {
        try {
          val streamCheckResult = StreamUtil.checkAndCreateKafkaTopic(stream)
          streamCheckResult match {
            case Left(err) => errors += err
            case _ =>
          }
        } catch {
          case e: TopicExistsException => errors += s"Cannot create kafka topic: ${e.getMessage}"
        }
      }
    }
  }

  /**
    * Checking and creating elasticsearch streams, if it's not exists
    *
    * @param errors - list of all errors
    * @param allEsStreams - all elasticsearch streams of instance
    */
  def checkEsStreams(errors: ArrayBuffer[String], allEsStreams: List[ESSjStream]) = {
    allEsStreams.foreach { (stream: ESSjStream) =>
      if (errors.isEmpty) {
        val streamCheckResult = StreamUtil.checkAndCreateEsStream(stream)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      }
    }
  }

  /**
    * Checking and creating sql tables, if it's not exists
    *
    * @param errors - list of all errors
    * @param allJdbcStreams - all jdbc streams of instance
    */
  def checkJdbcStreams(errors: ArrayBuffer[String], allJdbcStreams: List[JDBCSjStream]) = {
    allJdbcStreams.foreach { (stream: JDBCSjStream) =>
      if (errors.isEmpty) {
        val streamCheckResult = StreamUtil.checkAndCreateJdbcStream(stream)
        streamCheckResult match {
          case Left(err) => errors += err
          case _ =>
        }
      }
    }
  }

  /**
    * Check doubles in list
    *
    * @param list - list for checking
    * @return - true, if list contain doubles
    */
  def listHasDoubles(list: List[String]): Boolean = {
    list.map(x => (x, 1)).groupBy(_._1).map(x => x._2.reduce { (a, b) => (a._1, a._2 + b._2) }).exists(x => x._2 > 1)
  }

  /**
    * Get count of partition for streams
    *
    * @return - count of partition for each stream
    */
  def getPartitionForStreams(streams: Seq[SjStream]): Map[String, Int] = {
    Map(streams.map { stream =>
      stream.streamType match {
        case StreamConstants.tStream =>
          stream.name -> stream.asInstanceOf[TStreamSjStream].partitions
        case StreamConstants.kafka =>
          stream.name -> stream.asInstanceOf[KafkaSjStream].partitions
      }

    }: _*)
  }

  /**
    * Getting streams for such names
    *
    * @param streamNames Names of streams
    * @return Seq of streams
    */
  def getStreams(streamNames: List[String]): mutable.Buffer[SjStream] = {
    val streamsDAO = ConnectionRepository.getStreamService
    streamsDAO.getAll.filter(s => streamNames.contains(s.name))
  }

  /**
    * Getting stream for such name
    *
    * @param streamName Name of stream
    * @return SjStream object
    */
  def getStream(streamName: String) = {
    val streamsDAO = ConnectionRepository.getStreamService
    streamsDAO.get(streamName)
  }

  /**
    * Getting service names for all streams (must be one element in list)
    *
    * @param streams All streams
    * @return List of service-names
    */
  def getStreamServices(streams: Seq[SjStream]) = {
    streams.map(s => (s.service.name, 1)).groupBy(_._1).keys.toList
  }

  /**
    * Create execution plan for instance of module
    *
    * @param instance - instance for module
    * @return - execution plan of instance
    */
  def createExecutionPlan(instance: InstanceMetadata, partitionsCount: Map[String, Int]) = {
    var inputStreams: Array[String] = null
    instance.inputs match {
      case inputStreamArray: Array[String] =>
        inputStreams = inputStreamArray
      case _ =>
        inputStreams = Array(instance.inputs.asInstanceOf[String])
    }
    val inputs = inputStreams.map { input =>
      val mode = getStreamMode(input)
      val name = input.replaceAll("/split|/full", "")
      InputStream(name, mode, partitionsCount(name))
    }
    val parallelism = instance.parallelism.asInstanceOf[Int]
    val tasks = (0 until parallelism)
      .map(x => instance.name + "-task" + x)
      .map(x => x -> inputs)

    val executionPlan = mutable.Map[String, Task]()
    val streams = mutable.Map(inputs.map(x => x.name -> StreamProcess(0, x.partitionsCount)).toSeq: _*)

    var tasksNotProcessed = tasks.size
    tasks.foreach { task =>
      val list = task._2.map { inputStream =>
        val stream = streams(inputStream.name)
        val countFreePartitions = stream.countFreePartitions
        val startPartition = stream.currentPartition
        var endPartition = startPartition + countFreePartitions
        inputStream.mode match {
          case "full" => endPartition = startPartition + countFreePartitions
          case "split" =>
            val cntTaskStreamPartitions = countFreePartitions / tasksNotProcessed
            streams.update(inputStream.name, StreamProcess(startPartition + cntTaskStreamPartitions, countFreePartitions - cntTaskStreamPartitions))
            if (Math.abs(cntTaskStreamPartitions - countFreePartitions) >= cntTaskStreamPartitions) {
              endPartition = startPartition + cntTaskStreamPartitions
            }
        }

        inputStream.name -> Array(startPartition, endPartition - 1)
      }
      tasksNotProcessed -= 1
      val planTask = new Task
      planTask.inputs = mapAsJavaMap(Map(list.toSeq: _*))
      executionPlan.put(task._1, planTask)
    }
    val execPlan = new ExecutionPlan
    execPlan.tasks = mapAsJavaMap(executionPlan)
    execPlan
  }

  /**
    * Get mode from stream-name
    *
    * @param name - name of stream
    * @return - mode of stream
    */
  def getStreamMode(name: String) = {
    if (name.contains("/split")) {
      "split"
    } else if (name.contains("/full")) {
      "full"
    } else {
      "split"
    }
  }

}
