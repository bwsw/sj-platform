package com.bwsw.sj.engine.output.processing

import java.util.Calendar

import com.bwsw.common.{JsonSerializer, ElasticsearchClient}
import com.bwsw.sj.common.DAL.model.{ESService, SjStream}
import com.bwsw.sj.engine.core.entities.{Envelope, EsEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.elasticsearch.index.query.QueryBuilders

import scala.collection.mutable

class EsOutputProcessor[T <: AnyRef](outputStream: SjStream,
                        performanceMetrics: OutputStreamingPerformanceMetrics,
                        manager: OutputTaskManager)
  extends OutputProcessor[T](outputStream, performanceMetrics) {

  private val jsonSerializer = new JsonSerializer()
  private val esService = outputStream.service.asInstanceOf[ESService]
  private val esClient = openConnection()
  prepareIndex()
  
  private def openConnection(): ElasticsearchClient = {
    logger.info(s"Open a connection to elasticsearch at address: '${esService.provider.hosts}'.")
    val hosts = esService.provider.hosts.map(splitHost).toSet
    new ElasticsearchClient(hosts)
  }

  private def splitHost(host: String) = {
    val parts = host.split(":")

    (parts(0), parts(1).toInt)
  }

  def prepareIndex() = {
    logger.debug(s"Prepare an elasticsearch index.")
    esService.prepare()
    createIndexMapping()
  }

  private def createIndexMapping() = {
    val index = esService.index
    logger.debug(s"Create a mapping for an elasticsearch index: '$index'.")
    val streamName = outputStream.name
    val mappingSource = createMappingSource()

    esClient.createMapping(index, streamName, mappingSource)
  }

  private def createMappingSource() = {
    logger.debug(s"Create a mapping source for an elasticsearch index.")
    val fields = createGeneralFields()
    addCustomFields(fields)
    val mapping = Map("properties" -> fields)
    val mappingSource = jsonSerializer.serialize(mapping)

    mappingSource
  }

  private def createGeneralFields() = {
    logger.debug(s"Create a set of general fields for an elasticsearch index.")
    scala.collection.mutable.Map("txn" -> Map("type" -> "string"),
      "stream" -> Map("type" -> "string"),
      "partition" -> Map("type" -> "integer"))
  }

  private def addCustomFields(fields: mutable.Map[String, Map[String, String]]) = {
    logger.debug(s"Get a set of custom fields and add them.")
    val entity: EsEnvelope = _// TODO getOutputModule //
    val dateFields = entity.getDateFields()
    dateFields.foreach { field =>
      fields.put(field, Map("type" -> "date", "format" -> "epoch_millis"))
    }
  }

  def remove(envelope: TStreamEnvelope[T]) = {
    val transaction = envelope.id.toString.replaceAll("-", "")
    removeTransaction(transaction)
  }

  private def removeTransaction(transaction: String) = {
    val index = esService.index
    val streamName = outputStream.name
    logger.debug(s"Delete a transaction: '$transaction' from elasticsearch stream.")
    if (esClient.doesIndexExist(index)) {
      val query = QueryBuilders.matchQuery("txn", transaction)
      val outputData = esClient.search(index, streamName, query)
      outputData.getHits.foreach(hit => esClient.deleteDocumentByTypeAndId(index, streamName, hit.getId))
    }
  }

  def send(envelope: Envelope, inputEnvelope: TStreamEnvelope[T]) = {
    val esEnvelope = envelope.asInstanceOf[EsEnvelope]
    esEnvelope.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
    esEnvelope.transactionDateTime = s"${inputEnvelope.id}".dropRight(4)
    esEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
    esEnvelope.stream = inputEnvelope.stream
    esEnvelope.partition = inputEnvelope.partition
    esEnvelope.tags = inputEnvelope.tags

    logger.debug(s"Task: ${manager.taskName}. Write an output envelope to elasticsearch stream.")
    esClient.write(jsonSerializer.serialize(esEnvelope), esService.index, outputStream.name)
  }

  override def close() = {
    esClient.close()
  }
}
