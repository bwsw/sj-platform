package com.bwsw.sj.engine.output.task.engine

import java.util.Calendar
import java.util.concurrent.Callable

import com.bwsw.common.{ElasticsearchClient, JdbcClientBuilder, JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.{ESService, JDBCService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.TStreamTaskInputService
import com.bwsw.sj.engine.core.entities.{EsEnvelope, _}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
 * Provided methods are responsible for a basic execution logic of task of output module
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
  private val envelopeSerializer = new JsonSerializer(true)

  private lazy val outputStream = getOutput()
  protected lazy val environmentManager = createModuleEnvironmentManager()
  protected lazy val executor = manager.getExecutor(environmentManager).asInstanceOf[OutputStreamingExecutor]


  val taskInputService = new TStreamTaskInputService(manager, blockingQueue, checkpointGroup)
  protected val isNotOnlyCustomCheckpoint: Boolean
  private lazy val (esClient, esService) = openEsConnection(outputStream)
  private lazy val (jdbcClient, jdbcService) =  openJdbcConnection(outputStream)
  private var wasFirstCheckpoint = false
  private val byteSerializer = new ObjectSerializer()




  private def getOutput(): SjStream = {
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

  def openConnection(outputStream: SjStream) = {
  }

  /**
   * Open elasticsearch connection
   *
   * @param outputStream Output stream
   * @return ES Transport client and ES service of stream
   */
  private def openEsConnection(outputStream: SjStream) = {
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
    * Open JDBC connection
    *
    * @param outputStream Output stream
    * @return JDBC connection client and JDBC service of stream
    */
  private def openJdbcConnection(outputStream: SjStream) = {
    logger.info(s"Task: ${manager.taskName}. Open output JDBC connection.\n")
    val jdbcService: JDBCService = outputStream.service.asInstanceOf[JDBCService]
    val hosts = jdbcService.provider.hosts

    val client = JdbcClientBuilder.
      setHosts(hosts).
      setDriver(jdbcService.driver).
      setUsername(jdbcService.provider.login).
      setPassword(jdbcService.provider.password).
      setTable(outputStream.name).
      build()
    (client, jdbcService)
  }

  /**
    * It is in charge of running a basic execution logic of output task engine
    */
  override def call(): Unit = {

    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run output task engine in a separate thread of execution service\n")



    while (true) {
      val maybeEnvelope = blockingQueue.get(EngineLiterals.eventWaitTimeout)

      maybeEnvelope match {
        case Some(serializedEnvelope) =>
          println("envelope")
          processOutputEnvelope(serializedEnvelope)
        case _ =>
      }
      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }


  /**
   * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
    *
    * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside output module (not on the schedule) or not.
   */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean


  private def prepareES() = {
    esService.prepare()
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
    val mappingJson = envelopeSerializer.serialize(mapping)

    esClient.createMapping(index, streamName, mappingJson)
  }

  protected def afterReceivingEnvelope()

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
    if (esClient.doesIndexExist(index)) {
      val query = QueryBuilders.matchQuery("txn", transaction)
      val outputData = esClient.search(index, streamName, query)

      outputData.getHits.foreach { hit =>
        val id = hit.getId
        esClient.deleteDocumentByTypeAndId(index, streamName, id)
      }
    }
  }

  // todo private
  def processOutputEnvelope(serializedEnvelope: String) = {
    println("process")
    val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope).asInstanceOf[TStreamEnvelope]
    prepareES()
    removeFromES(envelope)
    afterReceivingEnvelope()
    taskInputService.registerEnvelope(envelope, performanceMetrics)
    logger.debug(s"Task: ${
      manager.taskName
    }. Invoke onMessage() handler\n")
    val outputEnvelopes: List[Envelope] = executor.onMessage(envelope)

    outputEnvelopes.foreach(outputEnvelope => registerAndSendOutputEnvelope(outputEnvelope, envelope))
  }


  private def registerAndSendOutputEnvelope(outputEnvelope: Envelope, inputEnvelope: TStreamEnvelope) = {
    println("register")
    registerOutputEnvelope(inputEnvelope.id.toString.replaceAll("-", ""), outputEnvelope)
    outputEnvelope match {
      case esEnvelope: EsEnvelope => writeToES(esEnvelope, inputEnvelope)
      case jdbcEnvelope: JdbcEnvelope => writeToJdbc(jdbcEnvelope)
      case _ =>
    }
  }

  private def registerOutputEnvelope(envelopeID: String, data: Envelope) = {
    val elementSize = byteSerializer.serialize(data).length
    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
  }

  /**
    * Writing entity to elasticsearch
    *
    * @param esEnvelope:
    * @param inputEnvelope:
    */
  private def writeToES(esEnvelope: EsEnvelope, inputEnvelope: TStreamEnvelope) = {
    println("write")
    esEnvelope.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
    // todo REMOVED 4 zeros from transactionDateTime
    esEnvelope.transactionDateTime = s"${inputEnvelope.id}".dropRight(4)
    esEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
    esEnvelope.stream = inputEnvelope.stream
    esEnvelope.partition = inputEnvelope.partition
    esEnvelope.tags = inputEnvelope.tags

    logger.debug(s"Task: ${manager.taskName}. Write output envelope to elasticsearch.")
    esClient.write(envelopeSerializer.serialize(esEnvelope), esService.index, outputStream.name)
    println("end")
  }

  /**
   * Writing entity to JDBC
   *
   * @param jdbcEnvelope: Output envelope for writing to JDBC
   */
  private def writeToJdbc(jdbcEnvelope: JdbcEnvelope) = {
    jdbcEnvelope.stream = ""
    jdbcClient.write(jdbcEnvelope)
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
