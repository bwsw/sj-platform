package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewTransactionProducerPolicy, Producer, ProducerTransaction}

import scala.collection._

/**
  * Provides an output stream that defined for each partition
  *
  * @author Kseniya Mikhaleva
  * @param producer Producer for specific output of stream
  */

class PartitionedOutput(producer: Producer,
                        performanceMetrics: PerformanceMetrics,
                        classLoader: ClassLoader) extends ModuleOutput(performanceMetrics, classLoader) {

  private val transactions = mutable.Map[Int, ProducerTransaction]()
  private val streamName = producer.stream.name

  def put(data: AnyRef, partition: Int): Unit = {
    val bytes = objectSerializer.serialize(data)
    logger.debug(s"Send a portion of data to stream: '$streamName' partition with number: '$partition'.")
    if (transactions.contains(partition)) {
      transactions(partition).send(bytes)
    }
    else {
      transactions(partition) = producer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened, partition)
      transactions(partition).send(bytes)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'.")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      transactions(partition).getTransactionID().toString,
      bytes.length
    )
  }
}