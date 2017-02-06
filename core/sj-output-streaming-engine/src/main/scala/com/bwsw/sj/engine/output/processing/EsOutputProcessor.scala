package com.bwsw.sj.engine.output.processing

import java.util.Calendar

import com.bwsw.common.{JsonSerializer, ElasticsearchClient}
import com.bwsw.sj.common.DAL.model.{ESService, SjStream}
import com.bwsw.sj.engine.core.entities.{Envelope, EsEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.elasticsearch.index.query.QueryBuilders

import scala.collection.mutable

class EsOutputProcessor(outputStream: SjStream,
                        performanceMetrics: OutputStreamingPerformanceMetrics,
                        manager: OutputTaskManager)
  extends OutputProcessor(outputStream, performanceMetrics) {

  private val envelopeSerializer = new JsonSerializer()
  private val esService = outputStream.service.asInstanceOf[ESService]
  private val esClient = openConnection()
  prepareIndex()
  
  private def openConnection(): ElasticsearchClient = {
    val hosts = esService.provider.hosts.map(splitHost).toSet
    new ElasticsearchClient(hosts)
  }

  private def splitHost(host: String) = {
    val parts = host.split(":")

    (parts(0), parts(1).toInt)
  }

  def prepareIndex() = {
    esService.prepare()
    createIndexMapping()
  }

  private def createIndexMapping() = {
    val index = esService.index
    logger.debug(s"Create the mapping for the elasticsearch index $index.")
    val streamName = outputStream.name
    val mappingSource = createMappingSource()

    esClient.createMapping(index, streamName, mappingSource)
  }

  private def createMappingSource() = {
    val fields = createGeneralFields()
    addCustomFields(fields)
    val mapping = Map("properties" -> fields)
    val mappingSource = envelopeSerializer.serialize(mapping)

    mappingSource
  }

  private def createGeneralFields() = {
    scala.collection.mutable.Map("txn" -> Map("type" -> "string"),
      "stream" -> Map("type" -> "string"),
      "partition" -> Map("type" -> "integer"))
  }

  private def addCustomFields(fields: mutable.Map[String, Map[String, String]]) = {
    val entity = manager.getOutputModuleEntity()
    val dateFields = entity.getDateFields()
    dateFields.foreach { field =>
      fields.put(field, Map("type" -> "date", "format" -> "epoch_millis"))
    }
  }

  def remove(envelope: TStreamEnvelope) = {
    val transaction = envelope.id.toString.replaceAll("-", "")
    removeTransaction(transaction)
  }

  private def removeTransaction(transaction: String) = {
    val index = esService.index
    val streamName = outputStream.name
    logger.info(s"Delete transaction $transaction from ES stream.")
    if (esClient.doesIndexExist(index)) {
      val query = QueryBuilders.matchQuery("txn", transaction)
      val outputData = esClient.search(index, streamName, query)
      outputData.getHits.foreach(hit => esClient.deleteDocumentByTypeAndId(index, streamName, hit.getId))
    }
  }

  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    val esEnvelope = envelope.asInstanceOf[EsEnvelope]
    esEnvelope.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
    esEnvelope.transactionDateTime = s"${inputEnvelope.id}".dropRight(4)
    esEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
    esEnvelope.stream = inputEnvelope.stream
    esEnvelope.partition = inputEnvelope.partition
    esEnvelope.tags = inputEnvelope.tags

    logger.debug(s"Task: ${manager.taskName}. Write output envelope to elasticsearch.")
    esClient.write(envelopeSerializer.serialize(esEnvelope), esService.index, outputStream.name)
  }
}
