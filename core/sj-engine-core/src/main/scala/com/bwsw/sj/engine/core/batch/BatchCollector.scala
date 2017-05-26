package com.bwsw.sj.engine.core.batch

import com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.engine.core.entities.{Batch, Envelope}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.Map

/**
  * Provides methods to gather batches that consist of envelopes
  *
  * @param instance           set of settings of a batch streaming module
  * @param performanceMetrics set of metrics that characterize performance of a batch streaming module
  * @author Kseniya Mikhaleva
  */
abstract class BatchCollector(protected val instance: BatchInstanceDomain,
                              performanceMetrics: BatchStreamingPerformanceMetrics) {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val streamRepository: GenericMongoRepository[StreamDomain] = ConnectionRepository.getStreamRepository
  private val inputs: Array[StreamDomain] = instance.getInputsWithoutStreamMode.map(x => streamRepository.get(x).get)
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





