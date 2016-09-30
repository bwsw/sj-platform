package com.bwsw.sj.engine.output.task.engine

import java.util.Calendar
import java.util.concurrent.Callable

import com.bwsw.common.{ElasticsearchClient, JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.{ESService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.TStreamTaskInputService
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
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
    val streamService = ConnectionRepository.getStreamService
    val outputs = instance.outputs
      .flatMap(x => streamService.get(x))

    val options = instance.getOptionsAsMap()

    new OutputEnvironmentManager(options, outputs)
  }

  /**
   * Open elasticsearch connection
   *
   * @param outputStream Output ES stream
   * @return ES Transport client and ES service of stream
   */
  private def openDbConnection(outputStream: SjStream) = {
    logger.info(s"Task: ${manager.taskName}. Open output elasticsearch connection.\n")
    val esService: ESService = outputStream.service.asInstanceOf[ESService]
    val hosts = esService.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)


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
      val maybeEnvelope = blockingQueue.get(EngineLiterals.eventWaitTimeout)

      maybeEnvelope match {
        case Some(serializedEnvelope) =>
          val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope).asInstanceOf[TStreamEnvelope]
          afterReceivingEnvelope()
          taskInputService.registerEnvelope(envelope, performanceMetrics)
          removeFromES(envelope)
          logger.debug(s"Task: ${
            manager.taskName
          }. Invoke onMessage() handler\n")
          val outputEnvelopes: List[Envelope] = executor.onMessage(envelope)
          outputEnvelopes.foreach(outputEnvelope => processOutputEnvelope(outputEnvelope, envelope))
        case _ =>
      }

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  private def prepareES() = {
    val index = esService.index
    logger.debug(s"Task: ${manager.taskName}. Prepare an elasticsearch index ${esService.index}")
    if (!client.doesIndexExist(index)) {
      client.createIndex(index)
    }

    createIndexMapping()
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

    client.createMapping(index, streamName, mappingJson)
  }

  protected def afterReceivingEnvelope(): Unit

  private def removeFromES(envelope: TStreamEnvelope) = {
    if (!wasFirstCheckpoint) {
      val transaction = envelope.id.toString.replaceAll("-", "")
      removeTxnFromES(transaction)
    }
  }

  private def removeTxnFromES(transaction: String) = {
    val index = esService.index
    val streamName = outputStream.name
    logger.info(s"Task: ${manager.taskName}. Delete transaction $transaction from ES stream.")
    if (client.doesIndexExist(index)) {
      val query = QueryBuilders.matchQuery("txn", transaction)
      val outputData = client.search(index, streamName, query)

      outputData.getHits.foreach { hit =>
        val id = hit.getId
        client.deleteDocumentByTypeAndId(index, streamName, id)
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
        esEnvelope.transactionDateTime = s"${inputEnvelope.id}"
        esEnvelope.transaction = inputEnvelope.id.toString.replaceAll("-", "")
        esEnvelope.stream = inputEnvelope.stream
        esEnvelope.partition = inputEnvelope.partition
        esEnvelope.tags = inputEnvelope.tags
        registerOutputEnvelope(esEnvelope.transaction, esEnvelope.data)
        logger.debug(s"Task: ${manager.taskName}. Write output envelope to elasticearch.")
        client.write(serializer.serialize(esEnvelope.data), esService.index, outputStream.name)
      case jdbcEnvelope: JdbcEnvelope => writeToJdbc(outputEnvelope)
      case _ =>
    }
  }

  private def registerOutputEnvelope(envelopeID: String, data: OutputData) = {
    val elementSize = outputEnvelopeSerializer.serialize(data).length
    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
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

