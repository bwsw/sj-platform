package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, ProducerTransaction}

/**
 * Provides an output stream that defined for stream in whole.
 * Recording of transaction occurs with the use of round-robin policy
 *
 *
 * @author Kseniya Mikhaleva
 * @param producer Producer for specific output of stream
 */

class RoundRobinOutput(producer: Producer[Array[Byte]],
                       performanceMetrics: PerformanceMetrics) extends ModuleOutput(performanceMetrics) {

  private var maybeTransaction: Option[ProducerTransaction[Array[Byte]]] = None
  private val streamName = producer.stream.getName

  def put(data: Array[Byte]) = {
    logger.debug(s"Send a portion of data to stream: '$streamName'")
    if (maybeTransaction.isDefined) {
      maybeTransaction.get.send(data)
    }
    else {
      maybeTransaction = Some(producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened))
      maybeTransaction.get.send(data)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      maybeTransaction.get.getTransactionID().toString,
      data.length
    )
  }
}