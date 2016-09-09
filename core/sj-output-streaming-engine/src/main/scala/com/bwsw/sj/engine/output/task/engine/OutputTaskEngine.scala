package com.bwsw.sj.engine.output.task.engine

import java.net.InetAddress
import java.util.concurrent.Callable
import java.util.{Calendar, UUID}

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.{ESService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineConstants
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.TStreamTaskInputService
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
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
 *
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
  private val serializer = new JsonSerializer()
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


  private def getOutput() = {
    val streamService = ConnectionRepository.getStreamService

    instance.outputs.flatMap(x => streamService.get(x)).head
  }

  private def createModuleEnvironmentManager() = {
    def getOptions() = {
      if (instance.options != null) {
        serializer.deserialize[Map[String, Any]](instance.options)
      } else {
        Map[String, Any]()
      } //todo remake this (or maybe remove) after completing SJ-2257
    }

    val streamService = ConnectionRepository.getStreamService
    val outputs = instance.outputs
      .flatMap(x => streamService.get(x))

    val options = getOptions()

    new OutputEnvironmentManager(options, outputs)
  }

  /**
   * Open elasticsearch connection
   *
   * @param outputStream Output ES stream
   * @return ES Transport client and ES service of stream
   */
  private def openDbConnection(outputStream: SjStream): (TransportClient, ESService) = {
    logger.info(s"Task: ${manager.taskName}. Open output elasticsearch connection.\n")
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
    prepareES()

    while (true) {
      val maybeEnvelope = blockingQueue.get(EngineConstants.eventWaitTimeout)

      if (maybeEnvelope != null) {
        val envelope = envelopeSerializer.deserialize[Envelope](maybeEnvelope).asInstanceOf[TStreamEnvelope]
        afterReceivingEnvelope()
        taskInputService.registerEnvelope(envelope, performanceMetrics)
        removeFromES(envelope)
        logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
        val outputEnvelopes: List[Envelope] = executor.onMessage(envelope)
        outputEnvelopes.foreach(outputEnvelope => processOutputEnvelope(outputEnvelope, envelope))
      }

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  private def prepareES() = {
    logger.debug(s"Task: ${manager.taskName}. Prepare an elasticsearch index ${esService.index}")
    if (!doesIndexExist) {
      createIndex()
    }
    createIndexMapping()
  }

  private def createIndex() = {
    val index = esService.index
    logger.debug(s"Task: ${manager.taskName}. Create an elasticsearch index $index")
    client.admin().indices().prepareCreate(index).execute().actionGet()
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
    val mappingJson = serializer.serialize(mapping)

    client.admin().indices()
      .preparePutMapping(index)
      .setType(streamName)
      .setSource(mappingJson)
      .execute()
      .actionGet()
  }

  protected def afterReceivingEnvelope(): Unit

  private def removeFromES(envelope: TStreamEnvelope) = {
    if (!wasFirstCheckpoint) {
      val txn = envelope.txnUUID.toString.replaceAll("-", "")
      removeTxnFromES(txn)
    }
  }

  private def removeTxnFromES(txn: String) = {
    val index = esService.index
    val streamName = outputStream.name
    logger.info(s"Task: ${manager.taskName}. Delete transaction $txn from ES stream.")
    if (doesIndexExist) {
      val request: SearchRequestBuilder = client
        .prepareSearch(index)
        .setTypes(streamName)
        .setQuery(QueryBuilders.matchQuery("txn", txn))
        .setSize(2000)
      val response: SearchResponse = request.execute().get()
      val outputData = response.getHits

      outputData.getHits.foreach { hit =>
        val id = hit.getId
        client.prepareDelete(index, streamName, id).execute().actionGet()
      }
    }
  }

  private def processOutputEnvelope(outputEnvelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    registerAndSendOutputEnvelope(outputEnvelope, inputEnvelope)
  }

  private def registerAndSendOutputEnvelope(outputEnvelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    outputEnvelope match {
      case esEnvelope: EsEnvelope =>
        esEnvelope.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
        esEnvelope.txnDateTime = s"${UUIDs.unixTimestamp(inputEnvelope.txnUUID)}"
        esEnvelope.txn = inputEnvelope.txnUUID.toString.replaceAll("-", "")
        esEnvelope.stream = inputEnvelope.stream
        esEnvelope.partition = inputEnvelope.partition
        esEnvelope.tags = inputEnvelope.tags
        registerOutputEnvelope(esEnvelope.txn, esEnvelope.data)
        writeEntityToES(esService.index, outputStream.name, esEnvelope.data, client)
      case jdbcEnvelope: JdbcEnvelope => writeToJdbc(outputEnvelope)
      case _ =>
    }
  }

  private def registerOutputEnvelope(envelopeID: String, data: OutputData) = {
    val elementSize = outputEnvelopeSerializer.serialize(data).length
    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
  }

  private def writeEntityToES(index: String, documentType: String, entity: OutputData, client: TransportClient) = {
    logger.debug(s"Task: ${manager.taskName}. Write output envelope to elasticearch.")
    val esData: String = serializer.serialize(entity)

    val request: IndexRequestBuilder = client.prepareIndex(index, documentType, UUID.randomUUID().toString)
    request.setSource(esData)
    val response = request.execute().actionGet()

    response
  }

  private def doesIndexExist = {
    val index = esService.index
    val doesExist = client.admin().indices().prepareExists(index).execute().actionGet().isExists

    doesExist
  }

  /**
   * Writing entity to JDBC
   *
   * @param envelope Output envelope for writing to sql database
   */
  private def writeToJdbc(envelope: Envelope) = {
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

