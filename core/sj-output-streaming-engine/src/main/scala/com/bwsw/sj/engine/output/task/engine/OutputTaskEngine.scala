package com.bwsw.sj.engine.output.task.engine

import java.net.InetAddress
import java.util.concurrent.Callable
import java.util.{Calendar, UUID}

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.{ESService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.ModuleConstants
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.{PersistentBlockingQueue, TStreamTaskInputService}
import com.bwsw.sj.engine.output.OutputDataFactory
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.datastax.driver.core.utils.UUIDs
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
 * Provides methods are responsible for a basic execution logic of task of output module
 * Created: 23/08/2016
 *
 * @param manager Manager of environment of task of output module
 * @param performanceMetrics Set of metrics that characterize performance of a output streaming module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @author Kseniya Mikhaleva
 */
abstract class OutputTaskEngine(protected val manager: OutputTaskManager,
                                performanceMetrics: OutputStreamingPerformanceMetrics,
                                blockingQueue: PersistentBlockingQueue) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"output-task-${manager.taskName}-engine")
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val checkpointGroup = new CheckpointGroup()
  protected val instance = manager.instance.asInstanceOf[OutputInstance]
  private val outputStream = getOutput()
  protected val environmentManager = createModuleEnvironmentManager()
  protected val executor = manager.getExecutor(environmentManager).asInstanceOf[OutputStreamingExecutor]
  val taskInputService = new TStreamTaskInputService(manager, blockingQueue, checkpointGroup)
  protected val isNotOnlyCustomCheckpoint: Boolean
  private val (client, esService) = openDbConnection(outputStream)
  private var wasFirstCheckpoint = false
  private val outputEnvelopeSerializer = new ObjectSerializer()
  private val serializer = new JsonSerializer()

  private def getOutput() = {
    val streamService = ConnectionRepository.getStreamService

    instance.outputs.flatMap(x => streamService.get(x)).head
  }

  private def createModuleEnvironmentManager() = {
    val streamService = ConnectionRepository.getStreamService
    val outputs = instance.outputs
      .flatMap(x => streamService.get(x))

    new OutputEnvironmentManager(serializer.deserialize[Map[String, Any]](instance.options), outputs)
  }

  /**
   * Open elasticsearch connection
   *
   * @param outputStream Output ES stream
   * @return ES Transport client and ES service of stream
   */
  private def openDbConnection(outputStream: SjStream): (TransportClient, ESService) = {
    logger.info(s"Task: ${OutputDataFactory.taskName}. Open output elasticsearch connection.\n")
    val esService: ESService = outputStream.service.asInstanceOf[ESService]
    val client: TransportClient = TransportClient.builder().build()
    esService.provider.hosts.foreach { host =>
      val parts = host.split(":")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(parts(0)), parts(1).toInt))
    }

    (client, esService)
  }

  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside output module (not on the schedule) or not.
   */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean

  /**
   * It is in charge of running a basic execution logic of output task engine
   */
  override def call(): Unit = {
    val envelopeSerializer = new JsonSerializer(true)
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run output task engine in a separate thread of execution service\n")
    val outputModuleEntity = manager.getOutputModuleEntity()
    createEsStream(esService.index, outputStream.name, outputModuleEntity, client)

    while (true) {
      val maybeEnvelope = blockingQueue.get(ModuleConstants.eventWaitTimeout) //todo maybe add to OutputInstance eventWaitTime parameter so PM will change too

      if (maybeEnvelope == null) {
        logger.debug(s"Task: ${manager.taskName}. Idle timeout: ${ModuleConstants.eventWaitTimeout} went out and nothing was received\n")
      } else {
        val envelope = envelopeSerializer.deserialize[Envelope](maybeEnvelope).asInstanceOf[TStreamEnvelope]
        afterReceivingEnvelope()
        taskInputService.registerEnvelope(envelope, performanceMetrics)
        deleteEnvelopeFromES(envelope)
        logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
        val outputEnvelopes: List[OutputEnvelope] = executor.onMessage(envelope)
        outputEnvelopes.foreach(outputEnvelope => processOutputEnvelope(outputEnvelope, envelope))
      }

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  /**
   * Create document type with mapping in elasticsearch
   *
   * @param index ES service index
   * @param streamName ES document type
   * @param entity User module output entity object
   * @param client ES Transport client
   * @return ES Response
   */
  private def createEsStream(index: String, streamName: String, entity: OutputEntity, client: TransportClient) = {
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Create elasticsearch index $index")
    val isIndexExist = client.admin().indices().prepareExists(index).execute().actionGet()
    if (!isIndexExist.isExists) {
      client.admin().indices().prepareCreate(index).execute().actionGet()
    }

    val fields = scala.collection.mutable.Map("txn" -> Map("type" -> "string"),
      "stream" -> Map("type" -> "string"),
      "partition" -> Map("type" -> "integer"))
    val dateFields: Array[String] = entity.getDateFields()
    dateFields.foreach { field =>
      fields.put(field, Map("type" -> "date", "format" -> "epoch_millis"))
    }
    val mapping = Map("properties" -> fields)
    val mappingJson = serializer.serialize(mapping)

    client.admin().indices()
      .preparePutMapping(index)
      .setType(streamName)
      .setSource(mappingJson)
      .execute()
      .actionGet()
  }

  protected def afterReceivingEnvelope(): Unit

  private def deleteEnvelopeFromES(envelope: TStreamEnvelope) = {
    if (!wasFirstCheckpoint) {
      deleteTransactionFromES(envelope, esService.index, outputStream.name, client)
    }
  }

  /**
   * Delete transactions from ES stream from last checkpoint
   *
   */
  private def deleteTransactionFromES(envelope: TStreamEnvelope, index: String, documentType: String, client: TransportClient) = {
    val txn = envelope.txnUUID.toString.replaceAll("-", "")

    logger.info(s"Task: ${OutputDataFactory.taskName}. Delete transaction $txn from ES stream.")
    val isIndexExist = client.admin().indices().prepareExists(index).execute().actionGet()
    if (isIndexExist.isExists) {
      val request: SearchRequestBuilder = client
        .prepareSearch(index)
        .setTypes(documentType)
        .setQuery(QueryBuilders.matchQuery("txn", txn))
        .setSize(2000)
      val response: SearchResponse = request.execute().get()
      val outputData = response.getHits

      outputData.getHits.foreach { hit =>
        val id = hit.getId
        client.prepareDelete(index, documentType, id).execute().actionGet()
      }
    }
  }

  private def processOutputEnvelope(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope) = {
    registerOutputEnvelope(outputEnvelope, inputEnvelope)
    sendOutputEnvelope(outputEnvelope, inputEnvelope)
  }

  private def registerOutputEnvelope(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope) = {
    performanceMetrics.addElementToOutputEnvelope(
      outputStream.name,
      inputEnvelope.txnUUID.toString,
      outputEnvelopeSerializer.serialize(outputEnvelope.data).length
    )
  }

  private def sendOutputEnvelope(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope) = {
    outputEnvelope.streamType match {
      case "elasticsearch-output" =>
        val entity = outputEnvelope.data.asInstanceOf[EsEntity]
        entity.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
        entity.txnDateTime = s"${UUIDs.unixTimestamp(inputEnvelope.txnUUID)}"
        entity.txn = inputEnvelope.txnUUID.toString.replaceAll("-", "")
        entity.stream = inputEnvelope.stream
        entity.partition = inputEnvelope.partition
        writeToElasticsearch(esService.index, outputStream.name, entity, client)
      case "jdbc-output" => writeToJdbc(outputEnvelope)
      case _ =>
    }
  }

  /**
   * Writing entity to ES
   *
   * @param index ES index
   * @param documentType ES document type (name of stream)
   * @param entity ES entity (data row)
   * @param client ES Transport client
   * @return Response from ES
   */
  private def writeToElasticsearch(index: String, documentType: String, entity: EsEntity, client: TransportClient) = {
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Write output envelope to elasticearch.")
    val esData: String = serializer.serialize(entity)

    val request: IndexRequestBuilder = client.prepareIndex(index, documentType, UUID.randomUUID().toString)
    request.setSource(esData)
    request.execute().actionGet()
  }

  /**
   * Writing entity to JDBC
   *
   * @param envelope Output envelope for writing to sql database
   */
  private def writeToJdbc(envelope: OutputEnvelope) = {
    //todo writing to JDBC
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  protected def doCheckpoint() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    checkpointGroup.checkpoint()
    prepareForNextCheckpoint()
    wasFirstCheckpoint = true
  }

  protected def prepareForNextCheckpoint(): Unit
}

