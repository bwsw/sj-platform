package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.module.reporting.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}

/**
 * Provides an output stream that defined for stream in whole.
 * Recording of transaction occurs with the use of round-robin policy
 * Created: 20/04/2016
 *
 * @author Kseniya Mikhaleva
 * @param producer Producer for specific output of stream
 */

class RoundRobinOutput(producer: BasicProducer[Array[Byte], Array[Byte]],
                       performanceMetrics: RegularStreamingPerformanceMetrics) extends ModuleOutput(performanceMetrics) {

  private var txn: Option[BasicProducerTransaction[Array[Byte], Array[Byte]]] = None
  private val streamName = producer.stream.getName

  def put(data: Array[Byte]) = {
    logger.debug(s"Send a portion of data to stream: '$streamName'")
    if (txn.isDefined) {
      txn.get.send(data)
    }
    else {
      txn = Some(producer.newTransaction(ProducerPolicies.errorIfOpen))
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
