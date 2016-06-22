package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.module.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}

import scala.collection._

/**
 * Provides an output stream that defined for each partition
 * Created: 20/04/2016
 *
 * @author Kseniya Mikhaleva
 * @param producer Producer for specific output of stream
 */

class PartitionedOutput(producer: BasicProducer[Array[Byte], Array[Byte]],
                        performanceMetrics: RegularStreamingPerformanceMetrics) extends ModuleOutput(performanceMetrics){

  private val txns = mutable.Map[Int, BasicProducerTransaction[Array[Byte], Array[Byte]]]()

  def put(data: Array[Byte], partition: Int) = {
    if (txns.contains(partition)) {
      performanceMetrics.addElementToOutputEnvelope(
        producer.stream.getName,
        txns(partition).getTxnUUID.toString,
        data.length
      )
      txns(partition).send(data)
    }
    else {
      txns(partition) = producer.newTransaction(ProducerPolicies.errorIfOpen, partition)
      performanceMetrics.addEnvelopeToOutputStream(
        producer.stream.getName,
        txns(partition).getTxnUUID.toString,
        mutable.ListBuffer(data.length)
      )
      txns(partition).send(data)
    }
  }
}
