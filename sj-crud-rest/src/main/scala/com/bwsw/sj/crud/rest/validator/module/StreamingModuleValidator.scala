package com.bwsw.sj.crud.rest.validator.module

import java.util.Calendar

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.crud.rest.entities.module.{ModuleSpecification, InstanceMetadata}
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

  var serviceDAO: GenericMongoService[Service] = null
  var instanceDAO: GenericMongoService[Instance] = null
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
  def createInstance(parameters: InstanceMetadata, partitionsCount: Map[String, Int], instance: Instance, streams: Set[SjStream]) = {
    val executionPlan = createExecutionPlan(parameters, partitionsCount)
    convertToModelInstance(instance, parameters)
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
  def validate(parameters: InstanceMetadata, specification: ModuleSpecification, validatedInstance: Instance) = {
    val validateParameters = parameters
    instanceDAO = ConnectionRepository.getInstanceService
    serviceDAO = ConnectionRepository.getServiceManager

    val errors = new ArrayBuffer[String]()

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

    if (parameters.jvmOptions.isEmpty) {
      errors += "Jvm-options attribute is empty."
    }

    if (parameters.coordinationService.isEmpty) {
      errors += "Coordination service attribute is empty."
    }

    val inputModes = parameters.inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += s"Unknown stream modes. Input streams must have modes 'split' or 'full'."
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if (parameters.inputs.length < inputsCardinality(0)) {
      errors += s"Count of inputs cannot be less than ${inputsCardinality(0)}."
    }
    if (parameters.inputs.length > inputsCardinality(1)) {
      errors += s"Count of inputs cannot be more than ${inputsCardinality(1)}."
    }
    if (listHasDoubles(parameters.inputs.toList)) {
      errors += s"Inputs is not unique."
    }
    val inputStreams = getStreams(parameters.inputs.toList.map(_.replaceAll("/split|/full", "")))
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
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be in: ${outputTypes.mkString(", ")}."
    }

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
      val service = allStreams.head.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'."
      } else {
        checkTStreams(errors, allStreams.filter(s => s.streamType.equals(tStream)))
      }
    }

    val kafkaStreams = allStreams.filter(s => s.streamType.equals(kafka))
    if (kafkaStreams.nonEmpty) {
      if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaService])) {
        errors += s"Service for kafka-streams must be 'KfkQ'."
      } else {
        checkKafkaStreams(errors, kafkaStreams)
      }
    }

    val esStreams = allStreams.filter(s => s.streamType.equals(esOutput))
    if (esStreams.nonEmpty) {
      if (esStreams.exists(s => !s.service.isInstanceOf[ESService])) {
        errors += s"Service for kafka-streams must be 'KfkQ'."
      } else {
        checkEsStreams(errors, esStreams)
      }
    }

    val jdbcStreams = allStreams.filter(s => s.streamType.equals(jdbcOutput))
    if (jdbcStreams.nonEmpty) {
      if (jdbcStreams.exists(s => !s.service.isInstanceOf[JDBCService])) {
        errors += s"Service for kafka-streams must be 'KfkQ'."
      } else {
        checkJdbcStreams(errors, jdbcStreams)
      }
    }

    val partitions = getPartitionForStreams(inputStreams)
    val minPartitionCount = partitions.values.min

    parameters.parallelism match {
      case parallelism: Int =>
        if (parallelism > minPartitionCount) {
          errors += s"Parallelism (${parameters.parallelism}) > minimum of partition count ($minPartitionCount) in all input stream."
        }
      case s: String =>
        if (!s.equals("max")) {
          errors += s"Parallelism must be int value or string 'max'."
        }
        validateParameters.parallelism = minPartitionCount
      case _ =>
        errors += "Unknown type of 'parallelism' parameter. Must be Int or String."
    }

    createInstance(validateParameters, partitions, validatedInstance, allStreams.filter(s => s.streamType.equals(tStream)).toSet)
    (errors, validatedInstance)
  }

  /**
    * Checking and creating t-streams, if streams is not exists
    *
    * @param errors - List of all errors
    * @param allTStreams - all t-streams of instance
    */
  def checkTStreams(errors: ArrayBuffer[String], allTStreams: mutable.Buffer[SjStream]) = {
    allTStreams.foreach { (stream: SjStream) =>
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
  def checkKafkaStreams(errors: ArrayBuffer[String], allKafkaStreams: mutable.Buffer[SjStream]) = {
    allKafkaStreams.foreach { (stream: SjStream) =>
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
  def checkEsStreams(errors: ArrayBuffer[String], allEsStreams: mutable.Buffer[SjStream]) = {
    allEsStreams.foreach { (stream: SjStream) =>
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
  def checkJdbcStreams(errors: ArrayBuffer[String], allJdbcStreams: mutable.Buffer[SjStream]) = {
    allJdbcStreams.foreach { (stream: SjStream) =>
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
      stream.name -> stream.partitions
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
    val inputs = instance.inputs.map { input =>
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
    val mode = name.substring(name.lastIndexOf("/") + 1)
    if (mode == null || mode.equals("")) {
      "split"
    } else {
      mode
    }
  }

}
