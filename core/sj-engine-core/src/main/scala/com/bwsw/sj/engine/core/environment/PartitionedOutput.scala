package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, Transaction}

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

  private val txns = mutable.Map[Int, Transaction[Array[Byte]]]()
  private val streamName = producer.stream.getName

  def put(data: Array[Byte], partition: Int) = {
    logger.debug(s"Send a portion of data to stream: '$streamName' partition with number: '$partition'")
    if (txns.contains(partition)) {
      txns(partition).send(data)
    }
    else {
      txns(partition) = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened, partition)
      txns(partition).send(data)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      txns(partition).getTransactionUUID().toString,
      data.length
    )
  }
}
