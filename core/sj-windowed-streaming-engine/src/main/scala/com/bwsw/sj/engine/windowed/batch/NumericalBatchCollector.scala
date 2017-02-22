package com.bwsw.sj.engine.windowed.batch

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.reporting.WindowedStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.windowed.BatchCollector
import org.slf4j.LoggerFactory

import scala.collection.mutable

class NumericalBatchCollector(instance: WindowedInstance,
                              performanceMetrics: WindowedStreamingPerformanceMetrics) extends BatchCollector(instance, performanceMetrics) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val countOfEnvelopesPerStream = mutable.Map(instance.getInputsWithoutStreamMode().map(x => (x, 0)): _*)

  def getBatchesToCollect(): Seq[String] = {
    countOfEnvelopesPerStream.filter(x => x._2 == EngineLiterals.everyNthCount).keys.toSeq
  }

  def afterReceivingEnvelope(envelope: Envelope) = {
    increaseCounter(envelope)
  }

  private def increaseCounter(envelope: Envelope) = {
    logger.debug(s"Increase count of envelopes of stream: ${envelope.stream}.")
    countOfEnvelopesPerStream(envelope.stream) += 1
  }

  def prepareForNextCollecting(streamName: String) = {
    resetCounter(streamName)
  }

  private def resetCounter(streamName: String) = {
    logger.debug(s"Reset a counter of envelopes to 0.")
    countOfEnvelopesPerStream(streamName) = 0
  }
}
