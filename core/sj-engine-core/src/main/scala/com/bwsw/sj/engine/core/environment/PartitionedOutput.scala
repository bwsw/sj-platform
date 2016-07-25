package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
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
                        performanceMetrics: PerformanceMetrics) extends ModuleOutput(performanceMetrics) {

  private val txns = mutable.Map[Int, BasicProducerTransaction[Array[Byte], Array[Byte]]]()
  private val streamName = producer.stream.getName

  def put(data: Array[Byte], partition: Int) = {
    logger.debug(s"Send a portion of data to stream: '$streamName' partition with number: '$partition'")
    if (txns.contains(partition)) {
      txns(partition).send(data)
    }
    else {
      txns(partition) = producer.newTransaction(ProducerPolicies.errorIfOpened, partition)
      txns(partition).send(data)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      txns(partition).getTxnUUID.toString,
      data.length
    )
  }
}
