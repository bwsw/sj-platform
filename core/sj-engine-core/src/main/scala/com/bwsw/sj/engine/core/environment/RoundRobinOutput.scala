package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, ProducerTransaction}

/**
  * Provides an output stream that defined for stream in whole.
  * Recording of transaction occurs with the use of round-robin policy
  *
  * @author Kseniya Mikhaleva
  * @param producer Producer for specific output of stream
  */

class RoundRobinOutput(producer: Producer,
                       performanceMetrics: PerformanceMetrics,
                       classLoader: ClassLoader) extends ModuleOutput(performanceMetrics, classLoader) {

  private var maybeTransaction: Option[ProducerTransaction] = None
  private val streamName = producer.stream.name

  def put(data: AnyRef) = {
    val bytes = objectSerializer.serialize(data)
    logger.debug(s"Send a portion of data to stream: '$streamName'.")
    if (maybeTransaction.isDefined) {
      maybeTransaction.get.send(bytes)
    }
    else {
      maybeTransaction = Some(producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened))
      maybeTransaction.get.send(bytes)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'.")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      maybeTransaction.get.getTransactionID().toString,
      bytes.length
    )
  }
}
