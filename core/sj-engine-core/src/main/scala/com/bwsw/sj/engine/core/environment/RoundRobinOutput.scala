package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, ProducerTransaction}

/**
 * Provides an output stream that defined for stream in whole.
 * Recording of transaction occurs with the use of round-robin policy
 * Created: 20/04/2016
 *
 * @author Kseniya Mikhaleva
 * @param producer Producer for specific output of stream
 */

class RoundRobinOutput(producer: Producer[Array[Byte]],
                       performanceMetrics: PerformanceMetrics) extends ModuleOutput(performanceMetrics) {

  private var txn: Option[ProducerTransaction[Array[Byte]]] = None
  private val streamName = producer.stream.getName

  def put(data: Array[Byte]) = {
    logger.debug(s"Send a portion of data to stream: '$streamName'")
    if (txn.isDefined) {
      txn.get.send(data)
    }
    else {
      txn = Some(producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened))
      txn.get.send(data)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      txn.get.getTxnUUID.toString,
      data.length
    )
  }
}
