package com.bwsw.sj.engine.core.batch

import com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.IBatchCollector
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.engine.core.entities.{Batch, Envelope}
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
  * Provides methods are responsible for a basic execution logic of task of batch module
  *
  * @param performanceMetrics Set of metrics that characterize performance of a batch streaming module
  * @author Kseniya Mikhaleva
  */
abstract class BatchCollector(protected val instance: BatchInstanceDomain,
                              performanceMetrics: BatchStreamingPerformanceMetrics) extends IBatchCollector {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val streamRepository = ConnectionRepository.getStreamRepository
  private val inputs = instance.getInputsWithoutStreamMode.map(x => streamRepository.get(x).get)
  private val currentBatchPerStream: Map[String, Batch] = createStorageOfBatches()

  private def createStorageOfBatches(): Map[String, Batch] = {
    inputs.map(x => (x.name, new Batch(x.name, x.tags))).toMap
  }

  def onReceive(envelope: Envelope): Unit = {
    logger.debug(s"Invoke onReceive() handler.")
    registerEnvelope(envelope)
    afterReceivingEnvelope(envelope)
  }

  private def registerEnvelope(envelope: Envelope): Unit = {
    logger.debug(s"Register an envelope: ${envelope.toString}.")
    currentBatchPerStream(envelope.stream).envelopes += envelope
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  def collectBatch(streamName: String): Batch = {
    logger.info(s"It's time to collect batch (stream: $streamName)\n")
    val batch = currentBatchPerStream(streamName).copy()
    currentBatchPerStream(streamName).envelopes.clear()
    prepareForNextCollecting(streamName)

    batch
  }

  protected def afterReceivingEnvelope(envelope: Envelope): Unit

  def getBatchesToCollect(): Seq[String]

  protected def prepareForNextCollecting(streamName: String): Unit
}





