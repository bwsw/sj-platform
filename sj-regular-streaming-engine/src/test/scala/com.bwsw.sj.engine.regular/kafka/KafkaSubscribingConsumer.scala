package com.bwsw.sj.engine.regular.kafka

import java.util.Properties
import java.util.concurrent.Executors

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.{StreamConstants, ModuleConstants}
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object KafkaSubscribingConsumer {
  def main(args: Array[String]) {
    val logger = LoggerFactory.getLogger(this.getClass)
    val executorService = Executors.newCachedThreadPool()
    var kafkaMessageAmount = 0
    val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(ModuleConstants.persistentBlockingQueue)
    val kafkaConsumer = createKafkaConsumer(List(("test", List(0, 2))))
    val timeout = 10

    var startTime = System.currentTimeMillis()
    var endTime = startTime

    val inputs = Map(("sflow-kafka-2", Array(0,2))).map(x => {
      val service = ConnectionRepository.getStreamService

      (service.get(x._1), x._2)
    })

    val kafkaInputs = inputs.filter(x => x._1.streamType == StreamConstants.kafka)

    executorService.execute(new Runnable() {
      def run() = {
        val inputKafkaSjStreams = kafkaInputs.map(x => (x._1.name, x._1.tags))
        val serializer = new JsonSerializer()

        while (true) {
          logger.debug(s"Waiting for records that consumed from kafka for $timeout milliseconds\n")
          val records = kafkaConsumer.poll(timeout)
          records.asScala.foreach(x => {

            blockingQueue.put(serializer.serialize({
              val envelope = new KafkaEnvelope()
              envelope.stream = x.topic()
              envelope.partition = x.partition()
              envelope.data = x.value()
              envelope.offset = x.offset()
              envelope.tags = inputKafkaSjStreams("sflow-kafka-2")
              envelope
            }))
          })
          kafkaMessageAmount += records.count()
          endTime = System.currentTimeMillis()

          if (startTime + 10000 < endTime) {
            println(s"time: ${(endTime - startTime) / 1000} seconds, messages: $kafkaMessageAmount")
            kafkaMessageAmount = 0
            startTime = System.currentTimeMillis()
            endTime = startTime
          }
        }
      }
    })

    //val maybeEnvelope = blockingQueue.get(timeout*10)
  }

  def createKafkaConsumer(topics: List[(String, List[Int])]) = {
    val props = new Properties()
    props.put("bootstrap.servers", "176.120.25.19:9092")
    props.put("enable.auto.commit", "false")
    props.put("auto.offset.reset", "latest")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    val topicPartitions = topics.flatMap(x => {
      (x._2.head to x._2.tail.head).map(y => new TopicPartition(x._1, y))
    }).asJava

    consumer.assign(topicPartitions)

    consumer
  }
}
