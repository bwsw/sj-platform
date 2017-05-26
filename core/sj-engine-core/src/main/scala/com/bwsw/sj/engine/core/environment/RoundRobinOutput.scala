package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, Producer, ProducerTransaction}

/**
  * Provides an output stream that defined for stream in whole.
  * Recording of transaction goes with the use of round-robin policy
  *
  * @param producer           producer of specific output
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  * @param classLoader        it is needed for loading some custom classes from module jar to serialize/deserialize envelope data
  *                           (ref. [[TStreamEnvelope.data]] or [[KafkaEnvelope.data]])
  * @author Kseniya Mikhaleva
  */

class RoundRobinOutput(producer: Producer,
                       performanceMetrics: PerformanceMetrics,
                       classLoader: ClassLoader) extends ModuleOutput(performanceMetrics, classLoader) {

  private var maybeTransaction: Option[ProducerTransaction] = None
  private val streamName = producer.stream.name

  def put(data: AnyRef): Unit = {
    val bytes = objectSerializer.serialize(data)
    logger.debug(s"Send a portion of data to stream: '$streamName'.")
    if (maybeTransaction.isDefined) {
      maybeTransaction.get.send(bytes)
    }
    else {
      maybeTransaction = Some(producer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened))
      maybeTransaction.get.send(bytes)
    }

    logger.debug(s"Add an element to output envelope of output stream:  '$streamName'.")
    performanceMetrics.addElementToOutputEnvelope(
      streamName,
      maybeTransaction.get.getTransactionID.toString,
      bytes.length
    )
  }
}
