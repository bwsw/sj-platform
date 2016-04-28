package com.bwsw.sj.crud.rest.validator.module

import java.net.{InetSocketAddress, URI}

import com.aerospike.client.Host
import com.bwsw.sj.common.DAL.ConnectionRepository
import com.bwsw.sj.common.entities.{Service, SjStream, RegularInstanceMetadata}
import com.bwsw.tstreams.coordination.Coordinator
import com.bwsw.tstreams.data.IStorage
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageOptions, AerospikeStorageFactory}
import com.bwsw.tstreams.data.cassandra.{CassandraStorageFactory, CassandraStorageOptions}
import com.bwsw.tstreams.metadata.MetadataStorageFactory
import com.bwsw.tstreams.services.BasicStreamService
import org.redisson.{Redisson, Config}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/**
  * Trait of validator for modules
  * Created: 12/04/2016
  *
  * @author Kseniya Tomskikh
  */
abstract class StreamingModuleValidator {
  import com.bwsw.sj.common.module.ModuleConstants._

  /**
    * Validating input parameters for streaming module
    *
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  def validate(parameters: RegularInstanceMetadata): (ArrayBuffer[String], Map[String, Int]) = {
    val instanceDAO = ConnectionRepository.getInstanceService
    val serviceDAO = ConnectionRepository.getServiceManager
    val providerDAO = ConnectionRepository.getProviderService

    val errors = new ArrayBuffer[String]()

    val instance = instanceDAO.get(parameters.name)
    if (instance == null) {
      errors += s"Instance for name: ${parameters.name} is exist."
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
    val streamsService = checkStreams(allStreams)
    val serviceName: Service = streamsService.head
    val service = serviceDAO.get(serviceName.name)
    if (streamsService.size != 1) {
      errors += s"All streams should have the same service."
    } else {
      if (service != null) {
        if (!service.serviceType.equals("TstrQ")) {
          errors += s"Service for streams must be 'TstrQ'."
        }
      } else {
        errors += s"Service $serviceName not found."
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
      case _ =>
        errors += "Unknown type of 'parallelism' parameter. Must be Int or String."
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

    /*val startFrom = parameters.startFrom
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
      }
    }

    if (service.isDefined) {
      val metadataProvider = providerDAO.retrieve(service.get.metadataProvider).get
      val hosts = metadataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt))
      val metadataStorage = (new MetadataStorageFactory).getInstance(hosts, service.get.metadataNamespace)

      val dataProvider = providerDAO.retrieve(service.get.dataProvider).get
      var dataStorage: IStorage[Array[Byte]] = null
      if (dataProvider.providerType.equals("cassandra")) {
        val options = new CassandraStorageOptions(
          dataProvider.hosts.map(s => new InetSocketAddress(s.split(":")(0), s.split(":")(1).toInt)),
          service.get.dataNamespace
        )
        dataStorage = (new CassandraStorageFactory).getInstance(options)
      } else if (dataProvider.providerType.equals("aerospike")) {
        val options = new AerospikeStorageOptions(
          service.get.dataNamespace,
          dataProvider.hosts.map(s => new Host(s.split(":")(0), s.split(":")(1).toInt))
        )
        dataStorage = (new AerospikeStorageFactory).getInstance(options)
      }

      val lockProvider = providerDAO.retrieve(service.get.lockProvider).get
      val redisConfig = new Config()
      redisConfig.useSingleServer().setAddress(lockProvider.hosts.head)
      val coordinator = new Coordinator(service.get.lockNamespace, Redisson.create(redisConfig))

      allStreams.foreach { (stream: Streams) =>
        val generatorType = stream.generator.head
        if (generatorType.equals("global") || generatorType.equals("per-stream")) {
          val generatorUrl = new URI(stream.generator(1))
          if (!generatorUrl.getScheme.equals("service-zk")) {
            errors += s"Generator have unknown service type: ${generatorUrl.getScheme}. Must be 'service-zk'."
          }
          val service = serviceDAO.retrieve(generatorUrl.getAuthority)
          service match {
            case Some(s) =>
              if (!s.serviceType.equals("ZKCoord")) {
                errors += s"Service for streams must be 'ZKCoord'."
              }
            case None => errors += s"Service ${generatorUrl.getHost} not found."
          }

          val n = stream.generator(2).toInt
          if (n < 0) {
            errors += s"Count instances of generator ($n) must be more than 1."
          }
        } else {
          if (!generatorType.equals("local")) {
            errors += s"Unknown generator type $generatorType for stream ${stream.name}."
          }
        }

        if (BasicStreamService.isExist(stream.name, metadataStorage)) {
          val tStream = BasicStreamService.loadStream[Array[Byte]](
            stream.name,
            metadataStorage,
            dataStorage,
            coordinator
          )
          if (tStream.getPartitions != stream.partitions.size) {
            errors += s"Partitions count mismatch"
          }
        } else {
          if (errors.isEmpty) {
            BasicStreamService.createStream(
              stream.name,
              stream.partitions.size,
              5000,
              "", metadataStorage,
              dataStorage,
              coordinator
            )
          }
        }

      }
    }*/

    (errors, partitions)
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
      stream.name -> stream.partitions.size
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
  def checkStreams(streams: Seq[SjStream]) = {
    streams.map(s => (s.service, 1)).groupBy(_._1).keys.toList
  }

}
