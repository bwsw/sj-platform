package com.bwsw.sj.engine.output.task.engine.handler

import java.util.Calendar

import com.bwsw.common.ElasticsearchClient
import com.bwsw.sj.common.DAL.model.{ESService, SjStream}
import com.bwsw.sj.engine.core.entities.{Envelope, EsEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.elasticsearch.index.query.QueryBuilders

/**
  * Created by diryavkin_dn on 07.11.16.
  */
class EsOutputHandler(outputStream: SjStream,
                      performanceMetrics: OutputStreamingPerformanceMetrics,
                      manager: OutputTaskManager)
                      extends OutputHandler(outputStream, performanceMetrics, manager) {

  val esService = outputStream.service.asInstanceOf[ESService]
  val esClient = openConnection()

  def openConnection(): ElasticsearchClient = {
    val hosts = esService.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    new ElasticsearchClient(hosts)
  }

  def prepare(inputEnvelope: TStreamEnvelope, wasFirstCheckpoint: Boolean) = {
    esService.prepare()
    createIndexMapping()
    remove(inputEnvelope, wasFirstCheckpoint: Boolean)
  }

  private def createIndexMapping() = {
    val index = esService.index
    logger.debug(s"Task: ${manager.taskName}. Create the mapping for the elasticsearch index $index")
    val streamName = outputStream.name
    val entity = manager.getOutputModuleEntity()
    val fields = scala.collection.mutable.Map("txn" -> Map("type" -> "string"),
      "stream" -> Map("type" -> "string"),
      "partition" -> Map("type" -> "integer"))
    val dateFields: Array[String] = entity.getDateFields()
    dateFields.foreach { field =>
      fields.put(field, Map("type" -> "date", "format" -> "epoch_millis"))
    }
    val mapping = Map("properties" -> fields)
    val mappingJson = envelopeSerializer.serialize(mapping)

    esClient.createMapping(index, streamName, mappingJson)
  }

  def remove(envelope: TStreamEnvelope, wasFirstCheckpoint: Boolean) = {
    if (!wasFirstCheckpoint) {
      val transaction = envelope.id.toString.replaceAll("-", "")
      removeTxnFromES(transaction)
    }
  }

  private def removeTxnFromES(transaction: String) = {
    val index = esService.index
    val streamName = outputStream.name
    logger.info(s"Task: ${manager.taskName}. Delete transaction $transaction from ES stream.")
    if (esClient.doesIndexExist(index)) {
      val query = QueryBuilders.matchQuery("txn", transaction)
      val outputData = esClient.search(index, streamName, query)
      outputData.getHits.foreach { hit =>
        val id = hit.getId
        esClient.deleteDocumentByTypeAndId(index, streamName, id)
      }
    }
  }

  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    val esEnvelope = envelope.asInstanceOf[EsEnvelope]
    esEnvelope.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
    // todo REMOVED 4 zeros from transactionDateTime (check it)
    esEnvelope.transactionDateTime = s"${inputEnvelope.id}".dropRight(4)
    esEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
    esEnvelope.stream = inputEnvelope.stream
    esEnvelope.partition = inputEnvelope.partition
    esEnvelope.tags = inputEnvelope.tags

    logger.debug(s"Task: ${manager.taskName}. Write output envelope to elasticsearch.")
    esClient.write(envelopeSerializer.serialize(esEnvelope), esService.index, outputStream.name)
  }
}
