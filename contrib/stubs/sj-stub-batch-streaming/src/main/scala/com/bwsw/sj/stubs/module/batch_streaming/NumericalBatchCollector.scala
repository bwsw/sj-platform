package com.bwsw.sj.stubs.module.batch_streaming

import com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.batch.{BatchCollector, BatchStreamingPerformanceMetrics}
import org.slf4j.LoggerFactory

import scala.collection.mutable

class NumericalBatchCollector(instance: BatchInstanceDomain,
                              performanceMetrics: BatchStreamingPerformanceMetrics) extends BatchCollector(instance, performanceMetrics) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val countOfEnvelopesPerStream = mutable.Map(instance.getInputsWithoutStreamMode().map(x => (x, 0)): _*)
  private val everyNthCount = 2

  def getBatchesToCollect(): Seq[String] = {
    countOfEnvelopesPerStream.filter(x => x._2 == everyNthCount).keys.toSeq
  }

  def afterReceivingEnvelope(envelope: Envelope): Unit = {
    increaseCounter(envelope)
  }

  private def increaseCounter(envelope: Envelope) = {
    countOfEnvelopesPerStream(envelope.stream) += 1
    logger.debug(s"Increase count of envelopes of stream: ${envelope.stream} to: ${countOfEnvelopesPerStream(envelope.stream)}.")
  }

  def prepareForNextCollecting(streamName: String): Unit = {
    resetCounter(streamName)
  }

  private def resetCounter(streamName: String) = {
    logger.debug(s"Reset a counter of envelopes to 0.")
    countOfEnvelopesPerStream(streamName) = 0
  }
}
