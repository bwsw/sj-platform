package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.module.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{ProducerPolicies, BasicProducerTransaction, BasicProducer}

import scala.collection.mutable

/**
 * Provides an output stream that defined for stream in whole.
 * Recording of transaction occurs with the use of round-robin policy
 * Created: 20/04/2016
 *
 * @author Kseniya Mikhaleva
 * @param producer Producer for specific output of stream
 */

class RoundRobinOutput(producer: BasicProducer[Array[Byte], Array[Byte]],
                       performanceMetrics: PerformanceMetrics) extends ModuleOutput(performanceMetrics) {

  private var txn: Option[BasicProducerTransaction[Array[Byte], Array[Byte]]] = None

  def put(data: Array[Byte]) = {
    if (txn.isDefined) {
      performanceMetrics.addElementToOutputEnvelope(
        producer.stream.getName,
        txn.get.getTxnUUID.toString,
        data.length
      )
      txn.get.send(data)
    }
    else {
      txn = Some(producer.newTransaction(ProducerPolicies.errorIfOpen))
      performanceMetrics.addEnvelopeToOutputStream(
        producer.stream.getName,
        txn.get.getTxnUUID.toString,
        mutable.ListBuffer(data.length)
      )
      txn.get.send(data)
    }
  }
}
