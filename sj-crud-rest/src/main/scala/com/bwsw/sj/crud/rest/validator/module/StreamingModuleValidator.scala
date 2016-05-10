package com.bwsw.sj.crud.rest.validator.module

import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.crud.rest.entities.InstanceMetadata
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageFactory, AerospikeStorageOptions}
import com.bwsw.tstreams.data.cassandra.{CassandraStorageFactory, CassandraStorageOptions}
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.bwsw.tstreams.services.BasicStreamService
import org.redisson.{Config, Redisson}

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
  var serviceDAO: GenericMongoService[Service] = null
  var instanceDAO: GenericMongoService[RegularInstance] = null
  val serializer: Serializer = new JsonSerializer

  case class InputStream(name: String, mode: String, partitionsCount: Int)

  case class StreamProcess(currentPartition: Int, countFreePartitions: Int)

  /**
    * Create entity of instance for saving to database
    * @param parameters - metadata of instance
    * @param partitionsCount - partitions count of input streams
    * @return
    */
  def createInstance(parameters: InstanceMetadata, partitionsCount: Map[String, Int], instance: RegularInstance) = {
    val executionPlan = createExecutionPlan(parameters, partitionsCount)
    convertToModelInstance(instance, parameters)
    instance.executionPlan = executionPlan
    instance
  }

  /**
    * Validating input parameters for streaming module
    *
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  def validate(parameters: InstanceMetadata, validatedInstance: RegularInstance) = {
    val validateParameters = parameters
    instanceDAO = ConnectionRepository.getInstanceService
    serviceDAO = ConnectionRepository.getServiceManager

    val errors = new ArrayBuffer[String]()

    val instance = instanceDAO.get(parameters.name)
    if (instance != null) {
      errors += s"Instance for name: ${parameters.name} is exist."
    }

    if (!stateManagementModes.contains(parameters.stateManagement)) {
      errors += s"Unknown value of state-management attribute: ${parameters.stateManagement}. " +
        s"State-management must be 'none' or 'ram' or 'rocks'."
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

    val startFrom = parameters.startFrom
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
      }
    }

    if (listHasDoubles(parameters.inputs.toList)) {
      errors += s"Inputs is not unique."
    }

    if (parameters.inputs.exists(s => !s.endsWith("/full") && !s.endsWith("/split"))) {
      errors += s"Inputs has incorrect name."
    }

    if (listHasDoubles(parameters.outputs.toList)) {
      errors += s"Outputs is not unique."
    }

    val inputStreams = getStreams(parameters.inputs.toList.map(_.replaceAll("/split|/full", "")))
    val outputStreams = getStreams(parameters.outputs.toList)

    val allStreams = inputStreams.union(outputStreams)
    val streamsServices = getStreamServices(allStreams)
    if (streamsServices.size != 1) {
      errors += s"All streams should have the same service."
    } else {
      val service = allStreams.head.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for streams must be 'TstrQ'."
      } else {
        errors.appendAll(checkAndCreateStreams(errors, service.asInstanceOf[TStreamService], allStreams))
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

    createInstance(validateParameters, partitions, validatedInstance)
    (errors, validatedInstance)
  }

  def checkAndCreateStreams(errors: ArrayBuffer[String], service: TStreamService, allStreams: mutable.Buffer[SjStream]) = {
    val metadataProvider = service.metadataProvider
    val hosts = metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt))
    val metadataStorage = (new MetadataStorageFactory).getInstance(hosts.toList, service.metadataNamespace)

    val dataProvider = service.dataProvider
    var dataStorage: IStorage[Array[Byte]] = null
    if (dataProvider.providerType.equals("cassandra")) {
      val options = new CassandraStorageOptions(
        dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)).toList,
        service.dataNamespace
      )
      dataStorage = (new CassandraStorageFactory).getInstance(options)
    } else if (dataProvider.providerType.equals("aerospike")) {
      val options = new AerospikeStorageOptions(
        service.dataNamespace,
        dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt)).toList
      )
      dataStorage = (new AerospikeStorageFactory).getInstance(options)
    }

    val lockProvider = service.lockProvider
    val redisConfig = new Config()
    redisConfig.useSingleServer().setAddress(lockProvider.hosts.head)
    val coordinator = new Coordinator(service.lockNamespace, Redisson.create(redisConfig))

    allStreams.foreach { (stream: SjStream) =>
      if (BasicStreamService.isExist(stream.name, metadataStorage)) {
        val tStream = BasicStreamService.loadStream[Array[Byte]](
          stream.name,
          metadataStorage,
          dataStorage,
          coordinator
        )
        if (tStream.getPartitions != stream.partitions) {
          errors += s"Partitions count mismatch"
        }
      } else {
        if (errors.isEmpty) {
          BasicStreamService.createStream(
            stream.name,
            stream.partitions,
            5000,
            "", metadataStorage,
            dataStorage,
            coordinator
          )
        }
      }

    }
    errors
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
    name.substring(name.lastIndexOf("/") + 1)
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
    modelInstance
  }

}
