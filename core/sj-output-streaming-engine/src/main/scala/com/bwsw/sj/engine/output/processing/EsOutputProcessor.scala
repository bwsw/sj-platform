package com.bwsw.sj.engine.output.processing

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.DAL.model.{ESService, SjStream}
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.es.ElasticsearchCommandBuilder


class EsOutputProcessor[T <: AnyRef](outputStream: SjStream,
                        performanceMetrics: OutputStreamingPerformanceMetrics,
                        manager: OutputTaskManager,
                                     entity: AnyRef)
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  private val esService = outputStream.service.asInstanceOf[ESService]
  private val esClient = openConnection()
  private val esCommandBuilder = new ElasticsearchCommandBuilder("txn", entity.asInstanceOf[Entity[String]])
  
  private def openConnection(): ElasticsearchClient = {
    logger.info(s"Open a connection to elasticsearch at address: '${esService.provider.hosts}'.")
    val hosts = esService.provider.hosts.map(splitHost).toSet
    new ElasticsearchClient(hosts)
  }

  private def splitHost(host: String) = {
    val parts = host.split(":")

    (parts(0), parts(1).toInt)
  }

  def remove(envelope: TStreamEnvelope[T]) = {
    val transaction = envelope.id
    val index = esService.index
    val streamName = outputStream.name
    logger.debug(s"Delete a transaction: '$transaction' from elasticsearch stream.")
    if (esClient.doesIndexExist(index)) {
      esCommandBuilder.buildRemove(index, streamName, transaction, esClient)
    }
  }


  def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]) = {

    val esFieldsValue = envelope.getFieldsValue

    val data = esCommandBuilder.buildInsert(inputEnvelope.id, esFieldsValue)
    logger.debug(s"Task: ${manager.taskName}. Write an output envelope to elasticsearch stream.")
    esClient.write(data, esService.index, outputStream.name)
  }

  override def close() = {
    esClient.close()
  }
}
