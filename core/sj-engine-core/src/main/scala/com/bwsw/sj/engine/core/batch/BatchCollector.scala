package com.bwsw.sj.engine.core.batch

import com.bwsw.sj.common._dal.model.module.BatchInstance
import com.bwsw.sj.common._dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.IBatchCollector
import com.bwsw.sj.engine.core.entities.{Batch, Envelope}
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
  * Provides methods are responsible for a basic execution logic of task of batch module
  *
  * @param performanceMetrics Set of metrics that characterize performance of a batch streaming module
  * @author Kseniya Mikhaleva
  */
abstract class BatchCollector(protected val instance: BatchInstance,
                              performanceMetrics: BatchStreamingPerformanceMetrics) extends IBatchCollector {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val streamDAO = ConnectionRepository.getStreamService
  private val inputs = instance.getInputsWithoutStreamMode().map(x => streamDAO.get(x).get)
  private val currentBatchPerStream: Map[String, Batch] = createStorageOfBatches()

  private def createStorageOfBatches() = {
    inputs.map(x => (x.name, new Batch(x.name, x.tags))).toMap
  }

  def onReceive(envelope: Envelope): Unit = {
    logger.debug(s"Invoke onReceive() handler.")
    registerEnvelope(envelope)
    afterReceivingEnvelope(envelope)
  }

  private def registerEnvelope(envelope: Envelope) = {
    logger.debug(s"Register an envelope: ${envelope.toString}.")
    currentBatchPerStream(envelope.stream).envelopes += envelope
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  def collectBatch(streamName: String) = {
    logger.info(s"It's time to collect batch (stream: $streamName)\n")
    val batch = currentBatchPerStream(streamName).copy()
    currentBatchPerStream(streamName).envelopes.clear()
    prepareForNextCollecting(streamName)

    batch
  }

  protected def afterReceivingEnvelope(envelope: Envelope)

  def getBatchesToCollect(): Seq[String]

  protected def prepareForNextCollecting(streamName: String)
}





