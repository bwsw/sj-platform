/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.batch.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile
import java.util.{Date, Properties}

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.{JsonSerializer, KafkaClient, ObjectSerializer}
import com.bwsw.sj.common.config.BenchmarkConfigNames
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, InstanceDomain, Task}
import com.bwsw.sj.common.dal.model.module.BatchSpecificationDomain
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.{KafkaServiceDomain, ServiceDomain, TStreamServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.utils._
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants._
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer
import com.bwsw.tstreams.agents.producer.Producer
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.TopicPartition
import scaldi.Injectable.inject

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object DataFactory {

  import com.bwsw.sj.common.SjModule._

  val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  private val config = ConfigFactory.load()
  private val zookeeperHosts = config.getString(BenchmarkConfigNames.zkHosts).split(",")
  private val kafkaHosts = config.getString(BenchmarkConfigNames.kafkaHosts)
  private val benchmarkPort = config.getInt(BenchmarkConfigNames.benchmarkPort)
  val inputStreamsType: String = config.getString(BenchmarkConfigNames.inputStreamTypes)
  private val agentsHost = "localhost"
  private val testNamespace = "test_namespace_for_batch_engine"
  private val instanceName = "test-instance-for-batch-engine"
  private val tstreamInputNamePrefix = "batch-tstream-input"
  private val tstreamOutputNamePrefix = "batch-tstream-output"
  private val kafkaInputNamePrefix = "batch-kafka-input"
  private val kafkaProviderName = "batch-kafka-test-provider"
  private val zookeeperProviderName = "batch-zookeeper-test-provider"
  private val tstreamServiceName = "batch-tstream-test-service"
  private val kafkaServiceName = "batch-kafka-test-service"
  private val zookeeperServiceName = "batch-zookeeper-test-service"
  private val replicationFactor = 1
  private var instanceInputs: Array[String] = Array()
  private var instanceOutputs: Array[String] = Array()
  private val task: Task = new Task()
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val zookeeperProvider = new ProviderDomain(
    zookeeperProviderName, zookeeperProviderName, zookeeperHosts, ProviderLiterals.zookeeperType, new Date())
  private val tstrqService = new TStreamServiceDomain(tstreamServiceName, tstreamServiceName, zookeeperProvider,
    TestStorageServer.defaultPrefix, TestStorageServer.defaultToken, new Date())
  private val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()
  private val storageClient = tstreamFactory.getStorageClient()


  private def setTStreamFactoryProperties() = {
    setAuthOptions(tstrqService)
    setCoordinationOptions(tstrqService)
    setBindHostForSubscribers()
  }

  private def setAuthOptions(tStreamService: TStreamServiceDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.Common.authenticationKey, tStreamService.token)
  }

  private def setCoordinationOptions(tStreamService: TStreamServiceDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.getConcatenatedHosts())
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.path, tStreamService.prefix)
  }

  private def setBindHostForSubscribers() = {
    tstreamFactory.setProperty(ConfigurationOptions.Consumer.Subscriber.bindHost, agentsHost)
  }

  def createProviders(providerService: GenericMongoRepository[ProviderDomain]): Unit = {
    val kafkaProvider = new ProviderDomain(
      kafkaProviderName, kafkaProviderName, kafkaHosts.split(","), ProviderLiterals.kafkaType, new Date())
    providerService.save(kafkaProvider)

    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoRepository[ProviderDomain]): Unit = {
    providerService.delete(kafkaProviderName)
    providerService.delete(zookeeperProviderName)
  }

  def createServices(serviceManager: GenericMongoRepository[ServiceDomain], providerService: GenericMongoRepository[ProviderDomain]): Unit = {
    val zkService = new ZKServiceDomain(zookeeperServiceName, zookeeperServiceName, zookeeperProvider,
      testNamespace, new Date())
    serviceManager.save(zkService)

    val kafkaProv = providerService.get(kafkaProviderName).get
    val kafkaService = new KafkaServiceDomain(
      kafkaServiceName, kafkaServiceName, kafkaProv, zookeeperProvider, new Date())
    serviceManager.save(kafkaService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoRepository[ServiceDomain]): Unit = {
    serviceManager.delete(kafkaServiceName)
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(tstreamServiceName)
  }

  def createStreams(repository: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain],
                    partitions: Int, _type: String, inputCount: Int, outputCount: Int): Unit = {
    require(partitions >= 1, "Partitions must be a positive integer")
    _type match {
      case `tStreamMode` =>
        (1 to inputCount).foreach(x => {
          createInputTStream(repository, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$tstreamInputNamePrefix$x/split"
          task.inputs.put(tstreamInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(repository, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case `kafkaMode` =>
        (1 to inputCount).foreach(x => {
          createKafkaStream(repository, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$kafkaInputNamePrefix$x/split"
          task.inputs.put(kafkaInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(repository, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case `commonMode` =>
        (1 to inputCount).foreach(x => {
          createInputTStream(repository, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$tstreamInputNamePrefix$x/split"
          task.inputs.put(tstreamInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))

          createKafkaStream(repository, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$kafkaInputNamePrefix$x/split"
          task.inputs.put(kafkaInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(repository, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tStreamMode, $kafkaMode, $commonMode")
    }
  }

  def deleteStreams(streamService: GenericMongoRepository[StreamDomain],
                    _type: String,
                    serviceManager: GenericMongoRepository[ServiceDomain],
                    inputCount: Int,
                    outputCount: Int): Unit = {
    _type match {
      case `tStreamMode` =>
        (1 to inputCount).foreach(x => deleteInputTStream(streamService, x.toString))
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case `kafkaMode` =>
        (1 to inputCount).foreach(x => deleteKafkaStream(streamService, serviceManager, x.toString))
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case `commonMode` =>
        (1 to inputCount).foreach(x => {
          deleteKafkaStream(streamService, serviceManager, x.toString)
          deleteInputTStream(streamService, x.toString)
        })
        (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tStreamMode, $kafkaMode, $commonMode")
    }
  }

  private def createInputTStream(repository: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], partitions: Int, suffix: String) = {
    val s1 = new TStreamStreamDomain(tstreamInputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamInputNamePrefix + suffix,
      false,
      Array("input"),
      creationDate = new Date()
    )

    repository.save(s1)

    storageClient.createStream(
      tstreamInputNamePrefix + suffix,
      partitions,
      1000 * 60,
      "description of test input tstream"
    )
  }

  private def createOutputTStream(repository: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], partitions: Int, suffix: String) = {
    val s2 = new TStreamStreamDomain(tstreamOutputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamOutputNamePrefix + suffix,
      false,
      Array("output", "some tags"),
      creationDate = new Date()
    )

    repository.save(s2)

    storageClient.createStream(
      tstreamOutputNamePrefix + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream"
    )
  }

  private def deleteInputTStream(streamService: GenericMongoRepository[StreamDomain], suffix: String) = {
    streamService.delete(tstreamInputNamePrefix + suffix)

    storageClient.deleteStream(tstreamInputNamePrefix + suffix)
  }

  private def deleteOutputTStream(streamService: GenericMongoRepository[StreamDomain], suffix: String) = {
    streamService.delete(tstreamOutputNamePrefix + suffix)

    storageClient.deleteStream(tstreamOutputNamePrefix + suffix)
  }

  private def createKafkaStream(repository: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], partitions: Int, suffix: String): Unit = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaServiceDomain]

    val kafkaStreamDomain = new KafkaStreamDomain(kafkaInputNamePrefix + suffix,
      kService,
      partitions,
      replicationFactor,
      kafkaInputNamePrefix + suffix,
      false,
      Array(kafkaInputNamePrefix),
      creationDate = new Date()
    )

    repository.save(kafkaStreamDomain)

    val zkServers = kService.zkProvider.hosts
    val kafkaClient = new KafkaClient(zkServers)

    kafkaClient.createTopic(kafkaStreamDomain.name, partitions, replicationFactor)
    kafkaClient.close()
  }

  private def deleteKafkaStream(streamService: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], suffix: String): Unit = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaServiceDomain]
    val zkServers = kService.zkProvider.hosts
    val kafkaClient = new KafkaClient(zkServers)
    kafkaClient.deleteTopic(kafkaInputNamePrefix + suffix)
    kafkaClient.close()

    streamService.delete(kafkaInputNamePrefix + suffix)
  }


  def createInstance(serviceManager: GenericMongoRepository[ServiceDomain],
                     instanceService: GenericMongoRepository[InstanceDomain],
                     window: Int,
                     slidingInterval: Int,
                     totalInputEnvelopes: Int,
                     stateManagement: String = EngineLiterals.noneStateMode,
                     stateFullCheckpoint: Int = 0): Unit = {
    import scala.collection.JavaConverters._

    val instance = new BatchInstance(
      name = instanceName,
      moduleType = EngineLiterals.batchStreamingType,
      moduleName = "batch-streaming-stub",
      moduleVersion = "1.0",
      engine = "com.bwsw.batch.streaming.engine-1.0",
      coordinationService = zookeeperServiceName,
      _status = EngineLiterals.started,
      inputs = instanceInputs,
      window = window,
      slidingInterval = slidingInterval,
      outputs = instanceOutputs,
      stateManagement = stateManagement,
      stateFullCheckpoint = stateFullCheckpoint,
      startFrom = EngineLiterals.oldestStartMode,
      options = s"$totalInputEnvelopes,$benchmarkPort",
      //executionPlan = new ExecutionPlan(Map((instanceName + "-task0", task), (instanceName + "-task1", task)).asJava) //for barriers test,
      executionPlan = new ExecutionPlan(Map(instanceName + "-task0" -> task).asJava),
      creationDate = new Date().toString
    )

    instanceService.save(instance.to)
  }

  def deleteInstance(instanceService: GenericMongoRepository[InstanceDomain]): Unit = {
    instanceService.delete(instanceName)
  }

  def loadModule(file: File, storage: FileStorage): Unit = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        val result = Try {
          var line = reader.readLine
          while (Option(line).isDefined) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        }
        reader.close()
        result match {
          case Success(_) =>
          case Failure(e) => throw e
        }
      }
    }

    val specification = serializer.deserialize[Map[String, Any]](builder.toString()) +
      ("className" -> classOf[BatchSpecificationDomain].getName)

    storage.put(file, file.getName, specification, "module")
  }

  def deleteModule(storage: FileStorage, filename: String): Boolean = {
    storage.delete(filename)
  }

  def createData(countTxns: Int,
                 countElementsPerTxn: Int,
                 streamService: GenericMongoRepository[StreamDomain],
                 _type: String,
                 streamsCount: Int): Unit = {

    require(types.contains(_type), s"Unknown type : ${_type}. Can be only: ${types.mkString(",")}")

    var number = 0
    val policy = producer.NewProducerTransactionPolicy.ErrorIfOpened

    def createTstreamData(suffix: String): Unit = {
      val producer = createProducer(tstreamInputNamePrefix + suffix, partitions)
      val s = System.currentTimeMillis()
      (0 until countTxns) foreach { _ =>
        val transaction = producer.newTransaction(policy)
        (0 until countElementsPerTxn) foreach { _ =>
          number += 1
          transaction.send(objectSerializer.serialize(number.asInstanceOf[Object]))
        }
        transaction.checkpoint()
      }

      println(s"producer time: ${(System.currentTimeMillis() - s) / 1000}")

      producer.stop()
    }

    def createKafkaData(suffix: String): Unit = {
      val props = new Properties()
      props.put("bootstrap.servers", kafkaHosts)
      props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

      val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
      var number = 0
      val s = System.currentTimeMillis()
      (0 until countTxns) foreach { _ =>
        number += 1
        val record = new ProducerRecord[Array[Byte], Array[Byte]](
          kafkaInputNamePrefix + suffix, Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object]))
        producer.send(record)
      }

      println(s"producer time: ${(System.currentTimeMillis() - s) / 1000}")
      producer.close()
    }

    if (Seq(tStreamMode, commonMode).contains(_type))
      (1 to streamsCount).foreach(x => createTstreamData(x.toString))

    if (Seq(kafkaMode, commonMode).contains(_type))
      (1 to streamsCount).foreach(x => createKafkaData(x.toString))
  }

  def createInputTstreamConsumer(partitions: Int, suffix: String): Consumer = {
    createConsumer(tstreamInputNamePrefix + suffix, partitions)
  }

  def createStateConsumer(streamService: GenericMongoRepository[StreamDomain]): Consumer = {
    val name = instanceName + "-task0" + "_state"
    val partitions = 1

    setStreamOptions(name, partitions)

    tstreamFactory.getConsumer(
      name,
      (0 until partitions).toSet,
      Oldest)
  }

  def createInputKafkaConsumer(inputCount: Int, partitionNumber: Int): KafkaConsumer[Array[Byte], Array[Byte]] = {

    val props = new Properties()
    props.put("bootstrap.servers", kafkaHosts)
    props.put("enable.auto.commit", "false")
    props.put("auto.offset.reset", "earliest")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    val topicPartitions = (0 until partitionNumber).flatMap(y => {
      (1 to inputCount).map(x => new TopicPartition(kafkaInputNamePrefix + x, y))
    }).asJava

    consumer.assign(topicPartitions)

    consumer
  }

  def createOutputConsumer(partitions: Int, suffix: String): Consumer = {
    createConsumer(tstreamOutputNamePrefix + suffix, partitions)
  }

  def createProducer(streamName: String, partitions: Int): Producer = {
    setStreamOptions(streamName, partitions)

    tstreamFactory.getProducer(
      "producer for " + streamName,
      (0 until partitions).toSet)
  }

  private def createConsumer(streamName: String, partitions: Int): Consumer = {
    setStreamOptions(streamName, partitions)

    tstreamFactory.getConsumer(
      streamName,
      (0 until partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(streamName: String, partitions: Int): TStreamsFactory = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, streamName)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, partitions)
  }
}
