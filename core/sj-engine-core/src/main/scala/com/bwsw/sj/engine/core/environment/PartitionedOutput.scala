package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, Producer, ProducerTransaction}

import scala.collection._

/**
  * Provides an output stream that defined for each partition
  *
  * @param producer           producer of specific output
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  * @param classLoader        it is needed for loading some custom classes from module jar to serialize/deserialize envelope data
  *                           (ref. [[TStreamEnvelope.data]] or [[KafkaEnvelope.data]])
  * @author Kseniya Mikhaleva
  */

class PartitionedOutput(producer: Producer,
                        performanceMetrics: PerformanceMetrics,
                        classLoader: ClassLoader)
  extends ModuleOutput(performanceMetrics, classLoader) {

  private val transactions = mutable.Map[Int, ProducerTransaction]()
  private val streamName = producer.stream.name

  def put(data: AnyRef, partition: Int): Unit = {
    val bytes = objectSerializer.serialize(data)
    logger.debug(s"Send a portion of data to stream: '$streamName' partition with number: '$partition'.")
    if (transactions.contains(partition)) {
      transactions(partition).send(bytes)
    }
    else {
      transactions(partition) = producer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened, partition)
      transactions(partition).send(bytes)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'.")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      transactions(partition).getTransactionID.toString,
      bytes.length
    )
  }
}