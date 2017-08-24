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
package com.bwsw.sj.engine.regular.module

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile
import java.util.{Date, Properties}

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.{JsonSerializer, KafkaClient, ObjectSerializer}
import com.bwsw.sj.common.config.BenchmarkConfigNames
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, InstanceDomain, Task}
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.{KafkaServiceDomain, ServiceDomain, TStreamServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance.RegularInstance
import com.bwsw.sj.common.utils._
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.sj.engine.regular.module.SjRegularBenchmarkConstants._
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.producer
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
  private val agentsHost = "localhost"
  private val testNamespace = "test_namespace_for_regular_engine"
  private val instanceName = "test-instance-for-regular-engine"
  private val tstreamInputNamePrefix = "regular-tstream-input"
  private val tstreamOutputNamePrefix = "regular-tstream-output"
  private val kafkaInputNamePrefix = "regular-kafka-input"
  private val kafkaProviderName = "regular-kafka-test-provider"
  private val zookeeperProviderName = "regular-zookeeper-test-provider"
  private val tstreamServiceName = "regular-tstream-test-service"
  private val kafkaServiceName = "regular-kafka-test-service"
  private val zookeeperServiceName = "regular-zookeeper-test-service"
  private val replicationFactor = 1
  private var instanceInputs: Array[String] = Array()
  private var instanceOutputs: Array[String] = Array()
  private val task: Task = new Task()
  private val serializer = new JsonSerializer()
  private val objectSerializer = new ObjectSerializer()
  private val zookeeperProvider = new ProviderDomain(zookeeperProviderName, zookeeperProviderName, zookeeperHosts,
    "", "", ProviderLiterals.zookeeperType, new Date())
  private val tstrqService = new TStreamServiceDomain(tstreamServiceName, tstreamServiceName, zookeeperProvider,
    TestStorageServer.defaultPrefix, TestStorageServer.defaultToken, creationDate = new Date())
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

  def createProviders(providerService: GenericMongoRepository[ProviderDomain]) = {
    val kafkaProvider = new ProviderDomain(kafkaProviderName, kafkaProviderName, kafkaHosts.split(","),
      "", "", ProviderLiterals.kafkaType, new Date())
    providerService.save(kafkaProvider)

    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoRepository[ProviderDomain]) = {
    providerService.delete(kafkaProviderName)
    providerService.delete(zookeeperProviderName)
  }

  def createServices(serviceManager: GenericMongoRepository[ServiceDomain], providerService: GenericMongoRepository[ProviderDomain]) = {
    val zkService = new ZKServiceDomain(zookeeperServiceName, zookeeperServiceName, zookeeperProvider,
      testNamespace, creationDate = new Date())
    serviceManager.save(zkService)

    val kafkaProv = providerService.get(kafkaProviderName).get
    val kafkaService = new KafkaServiceDomain(kafkaServiceName, kafkaServiceName, kafkaProv, zookeeperProvider,
      testNamespace, creationDate = new Date())
    serviceManager.save(kafkaService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoRepository[ServiceDomain]) = {
    serviceManager.delete(kafkaServiceName)
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(tstreamServiceName)
  }

  def createStreams(streamService: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain],
                    partitions: Int, _type: String, inputCount: Int, outputCount: Int) = {
    _type match {
      case `tstreamMode` =>
        (1 to inputCount).foreach(x => {
          createInputTStream(streamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$tstreamInputNamePrefix$x/split"
          task.inputs.put(tstreamInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(streamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case `kafkaMode` =>
        (1 to inputCount).foreach(x => {
          createKafkaStream(streamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$kafkaInputNamePrefix$x/split"
          task.inputs.put(kafkaInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(streamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case `commonMode` =>
        (1 to inputCount).foreach(x => {
          createInputTStream(streamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$tstreamInputNamePrefix$x/split"
          task.inputs.put(tstreamInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))

          createKafkaStream(streamService, serviceManager, partitions, x.toString)
          instanceInputs = instanceInputs :+ s"$kafkaInputNamePrefix$x/split"
          task.inputs.put(kafkaInputNamePrefix + x, Array(0, if (partitions > 1) partitions - 1 else 0))
        })
        (1 to outputCount).foreach(x => {
          createOutputTStream(streamService, serviceManager, partitions, x.toString)
          instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
        })
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tstreamMode, $kafkaMode, $commonMode")
    }
  }

  def deleteStreams(streamService: GenericMongoRepository[StreamDomain],
                    _type: String,
                    serviceManager: GenericMongoRepository[ServiceDomain],
                    inputCount: Int,
                    outputCount: Int) = {
    _type match {
      case `tstreamMode` =>
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
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tstreamMode, $kafkaMode, $commonMode")
    }
  }

  private def createInputTStream(streamService: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], partitions: Int, suffix: String) = {
    val s1 = new TStreamStreamDomain(tstreamInputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamInputNamePrefix + suffix,
      false,
      Array("input"),
      new Date()
    )

    streamService.save(s1)

    storageClient.createStream(
      tstreamInputNamePrefix + suffix,
      partitions,
      1000 * 60,
      "description of test input tstream"
    )
  }

  private def createOutputTStream(streamRepository: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], partitions: Int, suffix: String) = {
    val s2 = new TStreamStreamDomain(tstreamOutputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamOutputNamePrefix + suffix,
      false,
      Array("output", "some tags"),
      new Date()
    )

    streamRepository.save(s2)

    storageClient.createStream(
      tstreamOutputNamePrefix + suffix,
      partitions,
      10 * 60,
      tstreamOutputNamePrefix + suffix
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

  private def createKafkaStream(repository: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], partitions: Int, suffix: String) = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaServiceDomain]

    val kafkaStreamDomain = new KafkaStreamDomain(kafkaInputNamePrefix + suffix,
      kService,
      partitions,
      replicationFactor,
      kafkaInputNamePrefix + suffix,
      false,
      Array(kafkaInputNamePrefix),
      new Date()
    )

    repository.save(kafkaStreamDomain)

    val zkServers = kService.zkProvider.hosts
    val kafkaClient = new KafkaClient(zkServers)

    kafkaClient.createTopic(kafkaStreamDomain.name, partitions, replicationFactor)
    kafkaClient.close()
  }

  private def deleteKafkaStream(streamService: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], suffix: String) = {
    val kService = serviceManager.get(kafkaServiceName).get.asInstanceOf[KafkaServiceDomain]
    val zkServers = kService.zkProvider.hosts
    val kafkaClient = new KafkaClient(zkServers)
    kafkaClient.deleteTopic(kafkaInputNamePrefix + suffix)
    kafkaClient.close()

    streamService.delete(kafkaInputNamePrefix + suffix)
  }


  def createInstance(serviceManager: GenericMongoRepository[ServiceDomain],
                     instanceService: GenericMongoRepository[InstanceDomain],
                     checkpointInterval: Int,
                     totalInputElements: Int,
                     stateManagement: String = EngineLiterals.noneStateMode,
                     stateFullCheckpoint: Int = 0) = {
    import scala.collection.JavaConverters._

    val instance = new RegularInstance(
      name = instanceName,
      moduleType = EngineLiterals.regularStreamingType,
      moduleName = "regular-streaming-stub",
      moduleVersion = "1.0",
      engine = "com.bwsw.regular.streaming.engine-1.0",
      coordinationService = zookeeperServiceName,
      checkpointMode = EngineLiterals.everyNthMode,
      inputs = instanceInputs,
      outputs = instanceOutputs,
      checkpointInterval = checkpointInterval,
      stateManagement = stateManagement, options = s"$totalInputElements,$benchmarkPort",
      stateFullCheckpoint = stateFullCheckpoint,
      startFrom = EngineLiterals.oldestStartMode,
      executionPlan = new ExecutionPlan(Map(instanceName + "-task0" -> task, instanceName + "-task1" -> task).asJava),
      _status = EngineLiterals.started,
      creationDate = new Date().toString)

    instanceService.save(instance.to)
  }

  def deleteInstance(instanceService: GenericMongoRepository[InstanceDomain]) = {
    instanceService.delete(instanceName)
  }

  def loadModule(file: File, storage: FileStorage) = {
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

    val specification = serializer.deserialize[Map[String, Any]](builder.toString())

    storage.put(file, file.getName, specification, "module")
  }

  def deleteModule(storage: FileStorage, filename: String) = {
    storage.delete(filename)
  }

  def createData(countTxns: Int, countElements: Int, partitions: Int, _type: String, count: Int) = {
    val policy = producer.NewProducerTransactionPolicy.ErrorIfOpened
    val props = new Properties()
    props.put("bootstrap.servers", kafkaHosts)
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

    def createTstreamData(countTxns: Int, countElements: Int, suffix: String) = {
      val producer = createProducer(tstreamInputNamePrefix + suffix, partitions)
      var number = 0

      (0 until countTxns) foreach { (x: Int) =>
        val transaction = producer.newTransaction(policy)
        (0 until countElements) foreach { (y: Int) =>
          number += 1
          transaction.send(objectSerializer.serialize(number.asInstanceOf[Object]))
        }
        transaction.checkpoint()
      }

      producer.stop()
    }

    def createKafkaData(countTxns: Int, countElements: Int, suffix: String) = {
      val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
      var number = 0

      (0 until countTxns) foreach { (x: Int) =>
        (0 until countElements) foreach { (y: Int) =>
          number += 1
          val record = new ProducerRecord[Array[Byte], Array[Byte]](
            kafkaInputNamePrefix + suffix, Array[Byte](100), objectSerializer.serialize(number.asInstanceOf[Object])
          )
          producer.send(record)
        }
      }

      producer.close()
    }

    _type match {
      case `tstreamMode` =>
        (1 to count).foreach(x => createTstreamData(countTxns, countElements, x.toString))
      case `kafkaMode` =>
        (1 to count).foreach(x => createKafkaData(countTxns, countElements, x.toString))
      case `commonMode` =>
        (1 to count).foreach(x => {
          createTstreamData(countTxns, countElements, x.toString)
          createKafkaData(countTxns, countElements, x.toString)
        })
      case _ => throw new Exception(s"Unknown type : ${_type}. Can be only: $tstreamMode, $kafkaMode, $commonMode")
    }
  }

  def createInputTstreamConsumer(partitions: Int, suffix: String) = {
    createConsumer(tstreamInputNamePrefix + suffix, partitions)
  }

  def createStateConsumer(streamService: GenericMongoRepository[StreamDomain]) = {
    val name = instanceName + "-task0" + "_state"
    val partitions = 1

    setStreamOptions(name, partitions)

    tstreamFactory.getConsumer(
      name,
      (0 until partitions).toSet,
      Oldest)
  }

  def createInputKafkaConsumer(inputCount: Int, partitionNumber: Int) = {

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

  def createOutputConsumer(partitions: Int, suffix: String) = {
    createConsumer(tstreamOutputNamePrefix + suffix, partitions)
  }

  def createProducer(streamName: String, partitions: Int) = {
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

  protected def setStreamOptions(streamName: String, partitions: Int) = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, streamName)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, partitions)
  }
}