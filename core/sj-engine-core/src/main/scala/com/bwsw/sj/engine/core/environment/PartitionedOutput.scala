package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, ProducerTransaction}

import scala.collection._

/**
 * Provides an output stream that defined for each partition
 *
 *
 * @author Kseniya Mikhaleva
 * @param producer Producer for specific output of stream
 */

class PartitionedOutput(producer: Producer[Array[Byte]],
                        performanceMetrics: PerformanceMetrics) extends ModuleOutput(performanceMetrics) {

  private val transactions = mutable.Map[Int, ProducerTransaction[Array[Byte]]]()
  private val streamName = producer.stream.name

  def put(data: Array[Byte], partition: Int) = {
    logger.debug(s"Send a portion of data to stream: '$streamName' partition with number: '$partition'.")
    if (transactions.contains(partition)) {
      transactions(partition).send(data)
    }
    else {
      transactions(partition) = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened, partition)
      transactions(partition).send(data)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'.")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      transactions(partition).getTransactionID().toString,
      data.length
    )
  }
}
