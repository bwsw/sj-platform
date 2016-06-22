package com.bwsw.sj.engine.regular

import java.net.URLClassLoader
import java.util.Date
import java.util.concurrent.Executors

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.KafkaService
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

import com.bwsw.sj.common.module.RegularStreamingPerformanceMetrics
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.common.{ModuleConstants, StreamConstants}
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.{ModuleOutput, StatefulModuleEnvironmentManager, ModuleEnvironmentManager}
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.{StateStorage, RAMStateService}
import com.bwsw.tstreams.agents.consumer.Offsets.{DateTime, Newest, IOffset, Oldest}
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Object responsible for running a task of job that launches regular module
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val executorService = Executors.newCachedThreadPool()

  def main(args: Array[String]) {

    val manager = new TaskManager()
    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for regular module\n")

    logger.debug(s"Task: ${manager.taskName}. Start creating a t-stream producer to record performance reports\n")
    val reportStream = manager.getReportStream
    val reportProducer = manager.createProducer(reportStream)
    logger.debug(s"Task: ${manager.taskName}. Creation of t-stream producer is finished\n")

    val moduleJar = manager.getModuleJar

    val regularInstanceMetadata: RegularInstance = manager.getInstanceMetadata.asInstanceOf[RegularInstance]

    val executorClass = manager.getExecutorClass

    val outputTags = manager.getOutputTags

    val moduleTimer = new SjTimer()

    val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(ModuleConstants.persistentBlockingQueue)
    val checkpointGroup = new CheckpointGroup()

    val inputs = manager.inputs

    var consumersWithSubscribes: Option[Map[String, BasicSubscribingConsumer[Array[Byte], Array[Byte]]]] = None
    var offsetProducer: Option[BasicProducer[Array[Byte], Array[Byte]]] = None

    if (inputs.exists(x => x._1.streamType == StreamConstants.tStream)) {
      logger.debug(s"Task: ${manager.taskName}. Start creating subscribing consumers\n")
      consumersWithSubscribes = Some(inputs.filter(x => x._1.streamType == StreamConstants.tStream).map({
        x => manager.createSubscribingConsumer(x._1, x._2.toList, chooseOffset(regularInstanceMetadata.startFrom), blockingQueue)
      }).map(x => (x.name, x)).toMap)
      logger.debug(s"Task: ${manager.taskName}. Creation of subscribing consumers is finished\n")

      logger.debug(s"Task: ${manager.taskName}. Start adding subscribing consumers to checkpoint group\n")
      consumersWithSubscribes.get.foreach(x => checkpointGroup.add(x._1, x._2))
      logger.debug(s"Task: ${manager.taskName}. Adding subscribing consumers to checkpoint group is finished\n")
      logger.debug(s"Task: ${manager.taskName}. Launch subscribing consumers\n")
      consumersWithSubscribes.get.foreach(_._2.start())
      logger.debug(s"Task: ${manager.taskName}. Subscribing consumers are launched\n")
    }

    if (inputs.exists(x => x._1.streamType == StreamConstants.kafka)) {
      val kafkaInputs = inputs.filter(x => x._1.streamType == StreamConstants.kafka)
      logger.debug(s"Task: ${manager.taskName}. Start creating kafka consumers\n")
      val kafkaConsumer = manager.createKafkaConsumer(
        kafkaInputs.map(x => (x._1.name, x._2.toList)).toList,
        kafkaInputs.flatMap(_._1.service.asInstanceOf[KafkaService].provider.hosts).toList,
        regularInstanceMetadata.startFrom match {
          case "oldest" => "earliest"
          case _ => "latest"
        }
      )
      logger.debug(s"Task: ${manager.taskName}. Creation of kafka consumers is finished\n")

      logger.debug(s"Task: ${manager.taskName}. Launch kafka consumers that put consumed message, which are wrapped in envelope, into common queue \n")
      executorService.execute(new Runnable() {
        def run() = {
          val serializer = new JsonSerializer()
          val timeout = manager.kafkaSubscriberTimeout

          while (true) {
            logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $timeout milliseconds\n")
            val records = kafkaConsumer.poll(timeout)
            records.asScala.foreach(x => {
              val stream = ConnectionRepository.getStreamService.get(x.topic())

              blockingQueue.put(serializer.serialize({
                val envelope = new KafkaEnvelope()
                envelope.stream = stream.name
                envelope.partition = x.partition()
                envelope.data = x.value()
                envelope.offset = x.offset()
                envelope.tags = stream.tags
                envelope
              }))
            })
          }
        }
      })

      logger.debug(s"Task: ${manager.taskName}. Start creating a t-stream producer to record kafka offsets\n")
      val streamForOffsets = manager.getOffsetStream
      offsetProducer = Some(manager.createProducer(streamForOffsets))
      logger.debug(s"Task: ${manager.taskName}. Creation of t-stream producer is finished\n")
      logger.debug(s"Task: ${manager.taskName}. Start adding the t-stream producer to checkpoint group\n")
      checkpointGroup.add(offsetProducer.get.name, offsetProducer.get)
      logger.debug(s"Task: ${manager.taskName}. The t-stream producer is added to checkpoint group\n")
    }

    logger.debug(s"Task: ${manager.taskName}. Start creating t-stream producers for each output stream\n")
    val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] =
      regularInstanceMetadata.outputs
        .map(x => (x, ConnectionRepository.getStreamService.get(x)))
        .map(x => (x._1, manager.createProducer(x._2))).toMap
    logger.debug(s"Task: ${manager.taskName}. T-stream producers for each output stream are created\n")

    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    producers.foreach(x => checkpointGroup.add(x._2.name, x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")

    val classLoader = manager.getClassLoader(moduleJar.getAbsolutePath)

    val performanceMetrics = new RegularStreamingPerformanceMetrics(
      manager.taskName,
      manager.agentsHost,
      inputs.map(_._1.name).toArray ++ regularInstanceMetadata.outputs
    )

    logger.debug(s"Task: ${manager.taskName}. Launch a new thread to report performance metrics \n")
    executorService.execute(new Runnable() {
      def run() = {
        val taskNumber = manager.taskName.replace(s"${manager.instanceName}-task", "").toInt
        var report: String = null
        var reportTxn: BasicProducerTransaction[Array[Byte], Array[Byte]] = null
        while (true) {
          logger.info(s"Task: ${manager.taskName}. Wait ${regularInstanceMetadata.performanceReportingInterval} ms to report performance metrics\n")
          Thread.sleep(regularInstanceMetadata.performanceReportingInterval)
          report = performanceMetrics.getReport
          logger.info(s"Task: ${manager.taskName}. Performance metrics: $report \n")
          logger.debug(s"Task: ${manager.taskName}. Create a new txn for sending performance metrics\n")
          reportTxn = reportProducer.newTransaction(ProducerPolicies.errorIfOpen, taskNumber)
          logger.debug(s"Task: ${manager.taskName}. Send performance metrics\n")
          reportTxn.send(report.getBytes)
          logger.debug(s"Task: ${manager.taskName}. Do checkpoint of producer for performance reporting\n")
          reportProducer.checkpoint()
        }
      }
    })

    logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")
    try {
      runModule(moduleTimer,
        regularInstanceMetadata,
        blockingQueue,
        outputTags,
        classLoader,
        executorClass,
        producers,
        consumersWithSubscribes,
        manager,
        offsetProducer,
        checkpointGroup,
        performanceMetrics)
    }
    catch {
      case exception: Exception => {
        exception.printStackTrace()
        executorService.shutdownNow()
        System.exit(-1)
      }
    }
  }

  /**
   * Provides an imitation of streaming processing using blocking queue
   *
   * @param moduleTimer Provides a timer inside module
   * @param regularInstanceMetadata Launch parameters of module
   * @param blockingQueue Queue for keeping envelope
   * @param outputTags Keeps a tag (partitioned or round-robin output) corresponding to the output for each output stream
   * @param classLoader Allows loading an executor class
   * @param pathToExecutor Absolute class path to module class that implemented RegularStreamingExecutor
   * @param producers T-stream producers for sending data to output streams
   * @param consumers T-stream consumers to set local offset after fetching an envelope from queue
   * @param manager Allows managing an environment of task
   * @param offsetProducer T-stream producer to commit the offsets of last messages
   *                       that has successfully processed for each topic for each partition
   * @param checkpointGroup Group of producers and consumers which should do a checkpoint at the same time
   * @param performanceMetrics Set of metrics that characterize performance of module
   */
  private def runModule(moduleTimer: SjTimer,
                        regularInstanceMetadata: RegularInstance,
                        blockingQueue: PersistentBlockingQueue,
                        outputTags: mutable.Map[String, (String, ModuleOutput)],
                        classLoader: URLClassLoader,
                        pathToExecutor: String,
                        producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]],
                        consumers: Option[Map[String, BasicSubscribingConsumer[Array[Byte], Array[Byte]]]],
                        manager: TaskManager,
                        offsetProducer: Option[BasicProducer[Array[Byte], Array[Byte]]],
                        checkpointGroup: CheckpointGroup,
                        performanceMetrics: RegularStreamingPerformanceMetrics) = {
    /**
     * Json serializer for deserialization of envelope
     */
    val serializer = new JsonSerializer()
    serializer.setIgnoreUnknown(true)
    val objectSerializer = new ObjectSerializer()

    regularInstanceMetadata.stateManagement match {
      case "none" =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module without state\n")
        val moduleEnvironmentManager = new ModuleEnvironmentManager(
          serializer.deserialize[Map[String, Any]](regularInstanceMetadata.options),
          producers,
          regularInstanceMetadata.outputs
            .map(ConnectionRepository.getStreamService.get)
            .filter(_.tags != null),
          outputTags,
          moduleTimer,
          performanceMetrics
        )

        logger.debug(s"Task: ${manager.taskName}. Start loading of executor class from module jar\n")
        val executor = classLoader.loadClass(pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]
        logger.debug(s"Task: ${manager.taskName}. Create instance of executor class\n")

        logger.debug(s"Task: ${manager.taskName}. Invoke onInit() handler\n")
        executor.onInit()

        logger.debug(s"Task: ${manager.taskName}. Preparation of regular module without state is finished\n")
        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            logger.debug(s"Task: ${manager.taskName}. Start a regular module without state with time-interval checkpoint mode\n")
            val checkpointTimer = new SjTimer()
            checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.eventWaitTime)

              if (maybeEnvelope == null) {
                logger.debug(s"Task: ${manager.taskName}. Idle timeout: ${regularInstanceMetadata.eventWaitTime} went out and nothing was received\n")
                logger.debug(s"Task: ${manager.taskName}. Increase total idle time\n")
                performanceMetrics.increaseTotalIdleTime(regularInstanceMetadata.eventWaitTime)
                logger.debug(s"Task: ${manager.taskName}. Invoke onIdle() handler\n")
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)

                envelope.streamType match {
                  case StreamConstants.tStream =>
                    logger.info(s"Task: ${manager.taskName}. T-stream envelope is received\n")
                    val tStreamEnvelope = envelope.asInstanceOf[TStreamEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. " +
                      s"Change local offset of consumer: ${tStreamEnvelope.consumerName} to txn: ${tStreamEnvelope.txnUUID}\n")
                    consumers.get(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    performanceMetrics.addEnvelopeToInputStream(
                      tStreamEnvelope.stream,
                      tStreamEnvelope.data.map(_.length)
                    )
                  case StreamConstants.kafka =>
                    logger.info(s"Task: ${manager.taskName}. Kafka envelope is received\n")
                    val kafkaEnvelope = envelope.asInstanceOf[KafkaEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${kafkaEnvelope.stream} " +
                      s"for partition: ${kafkaEnvelope.partition} to ${kafkaEnvelope.offset}\n")
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    performanceMetrics.addEnvelopeToInputStream(
                      kafkaEnvelope.stream,
                      List(kafkaEnvelope.data.length)
                    )
                }

                logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
                executor.onMessage(envelope)

                if (checkpointTimer.isTime) {
                  logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
                  logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler\n")
                  executor.onBeforeCheckpoint()
                  if (offsetProducer.isDefined) {
                    logger.debug(s"Task: ${manager.taskName}. Save kafka offsets for each kafka input\n")
                    offsetProducer.get.newTransaction(ProducerPolicies.errorIfOpen)
                      .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  }
                  logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
                  checkpointGroup.commit()
                  outputTags.clear()
                  logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
                  executor.onAfterCheckpoint()
                  logger.debug(s"Task: ${manager.taskName}. Prepare a checkpoint timer for next cycle\n")
                  checkpointTimer.reset()
                  checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
                }

                if (moduleTimer.isTime) {
                  logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler\n")
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
          case "every-nth" =>
            logger.debug(s"Task: ${manager.taskName}. Start a regular module without state with every-nth checkpoint mode\n")
            logger.debug(s"Task: ${manager.taskName}. Set a counter of envelopes to 0\n")
            var countOfEnvelopes = 0
            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.eventWaitTime)

              if (maybeEnvelope == null) {
                logger.debug(s"Task: ${manager.taskName}. Idle timeout: ${regularInstanceMetadata.eventWaitTime} went out and nothing was received\n")
                logger.debug(s"Task: ${manager.taskName}. Increase total idle time\n")
                performanceMetrics.increaseTotalIdleTime(regularInstanceMetadata.eventWaitTime)
                logger.debug(s"Task: ${manager.taskName}. Invoke onIdle() handler\n")
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)
                countOfEnvelopes += 1
                logger.debug(s"Task: ${manager.taskName}. Increase count of envelopes to: $countOfEnvelopes\n")

                envelope.streamType match {
                  case StreamConstants.tStream =>
                    logger.info(s"Task: ${manager.taskName}. T-stream envelope is received\n")
                    val tStreamEnvelope = envelope.asInstanceOf[TStreamEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. " +
                      s"Change local offset of consumer: ${tStreamEnvelope.consumerName} to txn: ${tStreamEnvelope.txnUUID}\n")
                    consumers.get(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    performanceMetrics.addEnvelopeToInputStream(
                      tStreamEnvelope.stream,
                      tStreamEnvelope.data.map(_.length)
                    )
                  case StreamConstants.kafka =>
                    logger.info(s"Task: ${manager.taskName}. Kafka envelope is received\n")
                    val kafkaEnvelope = envelope.asInstanceOf[KafkaEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${kafkaEnvelope.stream} " +
                      s"for partition: ${kafkaEnvelope.partition} to ${kafkaEnvelope.offset}\n")
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    performanceMetrics.addEnvelopeToInputStream(
                      kafkaEnvelope.stream,
                      List(kafkaEnvelope.data.length)
                    )
                }

                logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
                executor.onMessage(envelope)

                if (countOfEnvelopes == regularInstanceMetadata.checkpointInterval) {
                  logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
                  logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler\n")
                  executor.onBeforeCheckpoint()
                  if (offsetProducer.isDefined) {
                    logger.debug(s"Task: ${manager.taskName}. Save kafka offsets for each kafka input\n")
                    offsetProducer.get.newTransaction(ProducerPolicies.errorIfOpen)
                      .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  }
                  logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
                  checkpointGroup.commit()
                  outputTags.clear()
                  logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
                  executor.onAfterCheckpoint()
                  logger.debug(s"Task: ${manager.taskName}. Reset a counter of envelopes to 0\n")
                  countOfEnvelopes = 0
                }

                if (moduleTimer.isTime) {
                  logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler\n")
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
        }

      case "ram" =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of regular module with state which is stored in RAM\n")
        var countOfCheckpoints = 1
        val streamForState = manager.getStateStream
        val stateProducer = manager.createProducer(streamForState)
        val stateConsumer = manager.createConsumer(streamForState, List(0, 0), Oldest)

        logger.debug(s"Task: ${manager.taskName}. Start adding state consumer and producer to checkpoint group\n")
        checkpointGroup.add(stateConsumer.name, stateConsumer)
        checkpointGroup.add(stateProducer.name, stateProducer)
        logger.debug(s"Task: ${manager.taskName}. Adding state consumer and producer to checkpoint group is finished\n")

        val stateService = new RAMStateService(stateProducer, stateConsumer)

        val moduleEnvironmentManager = new StatefulModuleEnvironmentManager(
          new StateStorage(stateService),
          serializer.deserialize[Map[String, Any]](regularInstanceMetadata.options),
          producers,
          regularInstanceMetadata.outputs
            .map(ConnectionRepository.getStreamService.get)
            .filter(_.tags != null),
          outputTags,
          moduleTimer,
          performanceMetrics
        )

        logger.debug(s"Task: ${manager.taskName}. Start loading of executor class from module jar\n")
        val executor = classLoader.loadClass(pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]
        logger.debug(s"Task: ${manager.taskName}. Instance of executor class is created\n")

        logger.debug(s"Task: ${manager.taskName}. Invoke onInit() handler\n")
        executor.onInit()

        logger.debug(s"Task: ${manager.taskName}. Preparation of regular module with state is finished\n")
        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            logger.debug(s"Task: ${manager.taskName}. Start a regular module with state with time-interval checkpoint mode\n")
            val checkpointTimer = new SjTimer()
            checkpointTimer.set(regularInstanceMetadata.checkpointInterval)

            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.eventWaitTime)

              if (maybeEnvelope == null) {
                logger.debug(s"Task: ${manager.taskName}. Idle timeout: ${regularInstanceMetadata.eventWaitTime} went out and nothing was received\n")
                logger.debug(s"Task: ${manager.taskName}. Increase total idle time\n")
                performanceMetrics.increaseTotalIdleTime(regularInstanceMetadata.eventWaitTime)
                logger.debug(s"Task: ${manager.taskName}. Invoke onIdle() handler\n")
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)

                envelope.streamType match {
                  case StreamConstants.tStream =>
                    logger.info(s"Task: ${manager.taskName}. T-stream envelope is received\n")
                    val tStreamEnvelope = envelope.asInstanceOf[TStreamEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. " +
                      s"Change local offset of consumer: ${tStreamEnvelope.consumerName} to txn: ${tStreamEnvelope.txnUUID}\n")
                    consumers.get(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    performanceMetrics.addEnvelopeToInputStream(
                      tStreamEnvelope.stream,
                      tStreamEnvelope.data.map(_.length)
                    )
                  case StreamConstants.kafka =>
                    logger.info(s"Task: ${manager.taskName}. Kafka envelope is received\n")
                    val kafkaEnvelope = envelope.asInstanceOf[KafkaEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${kafkaEnvelope.stream} " +
                      s"for partition: ${kafkaEnvelope.partition} to ${kafkaEnvelope.offset}\n")
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    performanceMetrics.addEnvelopeToInputStream(
                      kafkaEnvelope.stream,
                      List(kafkaEnvelope.data.length)
                    )
                }

                logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
                executor.onMessage(envelope)

                if (checkpointTimer.isTime) {
                  logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
                  logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler\n")
                  executor.onBeforeCheckpoint()

                  if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of a part of state\n")
                    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
                    executor.onBeforeStateSave(false)
                    stateService.savePartialState()
                    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
                    executor.onAfterStateSave(false)
                    countOfCheckpoints += 1
                  } else {
                    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of full state\n")
                    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
                    executor.onBeforeStateSave(true)
                    stateService.saveFullState()
                    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
                    executor.onAfterStateSave(true)
                    countOfCheckpoints = 1
                  }

                  if (offsetProducer.isDefined) {
                    logger.debug(s"Task: ${manager.taskName}. Save kafka offsets for each kafka input\n")
                    offsetProducer.get.newTransaction(ProducerPolicies.errorIfOpen)
                      .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  }
                  logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
                  checkpointGroup.commit()
                  logger.info(s"Set a number of state variables to ${stateService.getNumberOfVariables}\n")
                  performanceMetrics.setNumberOfStateVariables(stateService.getNumberOfVariables)
                  outputTags.clear()
                  logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
                  executor.onAfterCheckpoint()
                  logger.debug(s"Task: ${manager.taskName}. Prepare a checkpoint timer for next cycle\n")
                  checkpointTimer.reset()
                  checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
                }

                if (moduleTimer.isTime) {
                  logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler\n")
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }

          case "every-nth" =>
            logger.debug(s"Task: ${manager.taskName}. Start a regular module with state with every-nth checkpoint mode\n")
            logger.debug(s"Task: ${manager.taskName}. Set a counter of envelopes to 0\n")
            var countOfEnvelopes = 0

            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.eventWaitTime)

              if (maybeEnvelope == null) {
                logger.debug(s"Task: ${manager.taskName}. Idle timeout: ${regularInstanceMetadata.eventWaitTime} went out and nothing was received\n")
                logger.debug(s"Task: ${manager.taskName}. Increase total idle time\n")
                performanceMetrics.increaseTotalIdleTime(regularInstanceMetadata.eventWaitTime)
                logger.debug(s"Task: ${manager.taskName}. Invoke onIdle() handler\n")
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)
                countOfEnvelopes += 1
                logger.debug(s"Task: ${manager.taskName}. Increase count of envelopes to: $countOfEnvelopes\n")

                envelope.streamType match {
                  case StreamConstants.tStream =>
                    logger.info(s"Task: ${manager.taskName}. T-stream envelope is received\n")
                    val tStreamEnvelope = envelope.asInstanceOf[TStreamEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. " +
                      s"Change local offset of consumer: ${tStreamEnvelope.consumerName} to txn: ${tStreamEnvelope.txnUUID}\n")
                    consumers.get(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    performanceMetrics.addEnvelopeToInputStream(
                      tStreamEnvelope.stream,
                      tStreamEnvelope.data.map(_.length)
                    )
                  case StreamConstants.kafka =>
                    logger.info(s"Task: ${manager.taskName}. Kafka envelope is received\n")
                    val kafkaEnvelope = envelope.asInstanceOf[KafkaEnvelope]
                    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${kafkaEnvelope.stream} " +
                      s"for partition: ${kafkaEnvelope.partition} to ${kafkaEnvelope.offset}\n")
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    performanceMetrics.addEnvelopeToInputStream(
                      kafkaEnvelope.stream,
                      List(kafkaEnvelope.data.length)
                    )
                }

                logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
                executor.onMessage(envelope)

                if (countOfEnvelopes == regularInstanceMetadata.checkpointInterval) {
                  logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
                  logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler\n")
                  executor.onBeforeCheckpoint()

                  if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of a part of state\n")
                    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
                    executor.onBeforeStateSave(false)
                    stateService.savePartialState()
                    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
                    executor.onAfterStateSave(false)
                    countOfCheckpoints += 1
                  } else {
                    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of full state\n")
                    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
                    executor.onBeforeStateSave(true)
                    stateService.saveFullState()
                    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
                    executor.onAfterStateSave(true)
                    countOfCheckpoints = 1
                  }

                  if (offsetProducer.isDefined) {
                    logger.debug(s"Task: ${manager.taskName}. Save kafka offsets for each kafka input\n")
                    offsetProducer.get.newTransaction(ProducerPolicies.errorIfOpen)
                      .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  }
                  logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
                  checkpointGroup.commit()
                  logger.info(s"Set a number of state variables to ${stateService.getNumberOfVariables}\n")
                  performanceMetrics.setNumberOfStateVariables(stateService.getNumberOfVariables)
                  outputTags.clear()
                  logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
                  executor.onAfterCheckpoint()
                  logger.debug(s"Task: ${manager.taskName}. Reset the counter of envelopes to 0\n")
                  countOfEnvelopes = 0
                }

                if (moduleTimer.isTime) {
                  logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler\n")
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
        }
    }

  }

  /**
   * Chooses offset policy for t-streams consumers
   *
   * @param startFrom Offset policy name or specific date
   * @return Offset
   */
  private def chooseOffset(startFrom: String): IOffset = {
    logger.debug(s"Choose offset policy for t-streams consumer\n")
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }
}
