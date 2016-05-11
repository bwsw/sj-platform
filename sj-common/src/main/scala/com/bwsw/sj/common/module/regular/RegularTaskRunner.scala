package com.bwsw.sj.common.module.regular

import java.net.URLClassLoader
import java.util.Date

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.RegularInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.{ModuleConstants, StreamConstants}
import com.bwsw.sj.common.module.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.module.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.module.state.{RAMStateService, StateStorage}
import com.bwsw.sj.common.module.utils.SjTimer
import com.bwsw.sj.common.module.{PersistentBlockingQueue, TaskManager}
import com.bwsw.tstreams.agents.consumer.Offsets.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{BasicProducer, ProducerPolicies}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Object responsible for running a task of job that launches regular module
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner {

  def main(args: Array[String]) {
    val manager = new TaskManager()

    val moduleJar = manager.getModuleJar

    val regularInstanceMetadata: RegularInstance = manager.getInstanceMetadata

    val executorClass = manager.getExecutorClass

    val outputTags = manager.getOutputTags

    val moduleTimer = new SjTimer()

    val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(ModuleConstants.persistentBlockingQueue)
    val checkpointGroup = new CheckpointGroup()

    val inputs = regularInstanceMetadata.executionPlan.tasks.get(manager.taskName).inputs.asScala
      .map(x => {
      val service = ConnectionRepository.getStreamService

      (service.get(x._1), x._2)
    })

    var consumersWithSubscribes: Map[String, BasicSubscribingConsumer[Array[Byte], Array[Byte]]] = null

    if (inputs.exists(x => x._1.streamType == StreamConstants.streamTypes.head)) {
      consumersWithSubscribes = inputs.filter(x => x._1.streamType == StreamConstants.streamTypes.head).map({
        x => manager.createSubscribingConsumer(x._1, x._2.toList, chooseOffset(regularInstanceMetadata.startFrom), blockingQueue)
      }).map(x => (x.name, x)).toMap

      consumersWithSubscribes.foreach(x => checkpointGroup.add(x._1, x._2))
      consumersWithSubscribes.foreach(_._2.start())

    }

    if (inputs.exists(x => x._1.streamType == StreamConstants.streamTypes.last)) {
      val kafkaInputs = inputs.filter(x => x._1.streamType == StreamConstants.streamTypes.last)
      val kafkaConsumer = manager.createKafkaConsumer(kafkaInputs
        .map(x => (x._1.name, x._2.toList)).toList,
        kafkaInputs.flatMap(_._1.service.provider.hosts).toList,
        regularInstanceMetadata.startFrom match {
          case "oldest" => "earliest"
          case _ => "latest"
        }
      )

      new Thread(new Runnable {
        def run() = {
          val serializer = new JsonSerializer()

          while (true) {
            val records = kafkaConsumer.poll(10)
            records.asScala.foreach(x => {
              val stream = ConnectionRepository.getStreamService.get(x.topic())

              blockingQueue.put(serializer.serialize(
                new KafkaEnvelope(stream.name,
                  x.partition(),
                  x.value(),
                  x.offset(),
                  stream.tags
                )))
            })
          }
        }
      }).start()

    }

    val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] =
      regularInstanceMetadata.outputs
        .map(x => (x, ConnectionRepository.getStreamService.get(x)))
        .map(x => (x._1, manager.createProducer(x._2))).toMap

    val offsetProducer = manager.createOffsetProducer()

    producers.foreach(x => checkpointGroup.add(x._2.name, x._2))
    checkpointGroup.add(offsetProducer.name, offsetProducer)

    val classLoader = manager.getClassLoader(moduleJar.getAbsolutePath)

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
      checkpointGroup)
  }

  /**
   * Provides an imitation of streaming processing using blocking queue
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
   */
  private def runModule(moduleTimer: SjTimer,
                        regularInstanceMetadata: RegularInstance,
                        blockingQueue: PersistentBlockingQueue,
                        outputTags: mutable.Map[String, (String, Any)],
                        classLoader: URLClassLoader,
                        pathToExecutor: String,
                        producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]],
                        consumers: Map[String, BasicSubscribingConsumer[Array[Byte], Array[Byte]]],
                        manager: TaskManager,
                        offsetProducer: BasicProducer[Array[Byte], Array[Byte]],
                        checkpointGroup: CheckpointGroup) = {
    /**
     * Json serializer for deserialization of envelope
     */
    val serializer = new JsonSerializer()
    serializer.setIgnoreUnknown(true)
    val objectSerializer = new ObjectSerializer()

    regularInstanceMetadata.stateManagement match {
      case "none" =>
        val moduleEnvironmentManager = new ModuleEnvironmentManager(
          serializer.deserialize[Map[String, Any]](regularInstanceMetadata.options),
          producers,
          outputTags,
          moduleTimer
        )

        val executor = classLoader.loadClass(pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

        executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeEnvelope == null) {
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)

                envelope.streamType match {
                  case "t-stream" =>
                    val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](maybeEnvelope)
                    consumers(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    executor.onMessage(tStreamEnvelope)
                  case "kafka-stream" =>
                    val kafkaEnvelope = serializer.deserialize[KafkaEnvelope](maybeEnvelope)
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    executor.onMessage(kafkaEnvelope)
                }

                if (checkpointTimer.isTime) {
                  executor.onBeforeCheckpoint()
                  offsetProducer.newTransaction(ProducerPolicies.errorIfOpen)
                    .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  checkpointGroup.commit()
                  outputTags.clear()
                  executor.onAfterCheckpoint()
                  checkpointTimer.reset()
                  checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
                }

                if (moduleTimer.isTime) {
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
          case "every-nth" =>
            var countOfEnvelopes = 0
            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeEnvelope == null) {
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)
                countOfEnvelopes += 1

                envelope.streamType match {
                  case "t-stream" =>
                    val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](maybeEnvelope)
                    consumers(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    executor.onMessage(tStreamEnvelope)
                  case "kafka-stream" =>
                    val kafkaEnvelope = serializer.deserialize[KafkaEnvelope](maybeEnvelope)
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    executor.onMessage(kafkaEnvelope)
                }

                if (countOfEnvelopes == regularInstanceMetadata.checkpointInterval) {
                  executor.onBeforeCheckpoint()
                  offsetProducer.newTransaction(ProducerPolicies.errorIfOpen)
                    .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  checkpointGroup.commit()
                  outputTags.clear()
                  executor.onAfterCheckpoint()
                  countOfEnvelopes = 0
                }

                if (moduleTimer.isTime) {
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
        }

      case "ram" =>
        var countOfCheckpoints = 1
        val streamForState = ConnectionRepository.getStreamService.get(manager.taskName)
        val stateProducer = manager.createProducer(streamForState)
        val stateConsumer = manager.createConsumer(streamForState, List(0, streamForState.partitions), Oldest)

        checkpointGroup.add(stateConsumer.name, stateConsumer)
        checkpointGroup.add(stateProducer.name, stateProducer)

        val stateService = new RAMStateService(stateProducer, stateConsumer)

        val moduleEnvironmentManager = new StatefulModuleEnvironmentManager(
          new StateStorage(stateService),
          serializer.deserialize[Map[String, Any]](regularInstanceMetadata.options),
          producers,
          outputTags,
          moduleTimer
        )

        val executor = classLoader.loadClass(pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

        executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.set(regularInstanceMetadata.checkpointInterval)

            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeEnvelope == null) {
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)

                envelope.streamType match {
                  case "t-stream" =>
                    val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](maybeEnvelope)
                    consumers(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    executor.onMessage(tStreamEnvelope)
                  case "kafka-stream" =>
                    val kafkaEnvelope = serializer.deserialize[KafkaEnvelope](maybeEnvelope)
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    executor.onMessage(kafkaEnvelope)
                }

                if (checkpointTimer.isTime) {
                  if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                    executor.onBeforeStateSave(false)
                    stateService.checkpoint()
                    executor.onAfterStateSave(false)
                    countOfCheckpoints += 1
                  } else {
                    executor.onBeforeStateSave(true)
                    stateService.fullCheckpoint()
                    executor.onAfterStateSave(true)
                    countOfCheckpoints = 0
                  }

                  executor.onBeforeCheckpoint()
                  offsetProducer.newTransaction(ProducerPolicies.errorIfOpen)
                    .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  checkpointGroup.commit()
                  outputTags.clear()
                  executor.onAfterCheckpoint()

                  checkpointTimer.reset()
                  checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
                }

                if (moduleTimer.isTime) {
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }

          case "every-nth" =>
            var countOfEnvelopes = 0

            while (true) {

              val maybeEnvelope = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeEnvelope == null) {
                executor.onIdle()
              } else {
                val envelope = serializer.deserialize[Envelope](maybeEnvelope)
                countOfEnvelopes += 1

                envelope.streamType match {
                  case "t-stream" =>
                    val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](maybeEnvelope)
                    consumers(tStreamEnvelope.consumerName).setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
                    executor.onMessage(tStreamEnvelope)
                  case "kafka-stream" =>
                    val kafkaEnvelope = serializer.deserialize[KafkaEnvelope](maybeEnvelope)
                    manager.kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
                    executor.onMessage(kafkaEnvelope)
                }

                if (countOfEnvelopes == regularInstanceMetadata.checkpointInterval) {
                  if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                    stateService.checkpoint()
                    executor.onAfterStateSave(false)
                    countOfCheckpoints += 1
                  } else {
                    stateService.fullCheckpoint()
                    executor.onAfterStateSave(true)
                    countOfCheckpoints = 1
                  }

                  executor.onBeforeCheckpoint()
                  offsetProducer.newTransaction(ProducerPolicies.errorIfOpen)
                    .send(objectSerializer.serialize(manager.kafkaOffsetsStorage))
                  checkpointGroup.commit()
                  outputTags.clear()
                  executor.onAfterCheckpoint()

                  countOfEnvelopes = 0
                }

                if (moduleTimer.isTime) {
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
   * @param startFrom Offset policy name or specific date
   * @return Offset
   */
  private def chooseOffset(startFrom: String): IOffset = {
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }
}
