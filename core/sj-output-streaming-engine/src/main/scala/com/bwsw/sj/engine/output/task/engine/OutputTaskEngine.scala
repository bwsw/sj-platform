package com.bwsw.sj.engine.output.task.engine

import java.util.{Calendar, UUID}
import java.util.concurrent.Callable

import com.bwsw.common.{ElasticsearchClient, JdbcClientBuilder, JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.{ESService, JDBCService, JDBCSjStream, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.DAL.model.Service
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.TStreamTaskInputService
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.engine.Handler.{EsOutputHandler, JdbcOutputHandler, OutputHandler}
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
 * Provided methods are responsible for a basic execution logic of task of output module
 *
 * @param manager Manager of environment of task of output module
 * @param performanceMetrics Set of metrics that characterize performance of a output streaming module

 * @author Kseniya Mikhaleva
 */
abstract class OutputTaskEngine(protected val manager: OutputTaskManager,
                                performanceMetrics: OutputStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"output-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(EngineLiterals.persistentBlockingQueue)
  private val envelopeSerializer = new JsonSerializer()
  private val instance = manager.instance.asInstanceOf[OutputInstance]
  private val outputStream = getOutputStream
  protected val environmentManager = createModuleEnvironmentManager()
  private val executor = manager.getExecutor(environmentManager).asInstanceOf[OutputStreamingExecutor]
  val taskInputService = new TStreamTaskInputService(manager, blockingQueue)
  protected val isNotOnlyCustomCheckpoint: Boolean
//  private lazy val (client, service) = openConnection(outputStream)

  private val handler = createHandler(outputStream)

//  private lazy val (esClient, esService) = openEsConnection(outputStream)
//  private lazy val (jdbcClient, jdbcService) = openJdbcConnection(outputStream)

  private var wasFirstCheckpoint = false
//  private val byteSerializer = new ObjectSerializer()



  private def getOutputStream: SjStream = {
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


//  /**
//    * Open connection for output data storage.
//    *
//    * @param outputStream: stream provide data.
//    */
//  def openConnection(outputStream: SjStream):(OutputClient, Service) = {
//    val jdbcClient = new JdbcClient()
//    val jdbcService: JDBCService = outputStream.service.asInstanceOf[JDBCService]
//    (jdbcClient, jdbcService)
//  }

  def createHandler(outputStream: SjStream): OutputHandler = {
    outputStream.streamType match {
      case StreamLiterals.esOutputType =>
        new EsOutputHandler(outputStream, performanceMetrics, manager)
      case StreamLiterals.jdbcOutputType =>
        new JdbcOutputHandler(outputStream, performanceMetrics, manager)
    }
  }


  /**
    * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
    *
    * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside output module (not on the schedule) or not.
    */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean


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
          processOutputEnvelope(serializedEnvelope)
        case _ =>
      }
      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }


  /**
    * Handler of envelope.
    *
    * @param serializedEnvelope: original envelope.
    */
  // todo private
  private def processOutputEnvelope(serializedEnvelope: String) = {
// todo    afterReceivingEnvelope()
    val envelope = envelopeSerializer.deserialize[Envelope](serializedEnvelope).asInstanceOf[TStreamEnvelope]
    registerInputEnvelope(envelope)
    logger.debug(s"Task: ${manager.taskName}. Invoke onMessage() handler\n")
    val outputEnvelopes: List[Envelope] = executor.onMessage(envelope)
    handler.process(outputEnvelopes, envelope, wasFirstCheckpoint)
//    storagePrepare()
//    outputStream.streamType match {
//      case StreamLiterals.esOutputType =>
//        prepareES()
//        removeFromES(envelope)
//      case StreamLiterals.jdbcOutputType =>
//        removeFromJdbc(envelope)
//    }
//    outputEnvelopes.foreach(outputEnvelope => registerAndSendOutputEnvelope(outputEnvelope, envelope))
  }


  /**
    * Register received envelope in performance metrics.
    *
    * @param envelope: received data
    */
  private def registerInputEnvelope(envelope: Envelope) = {
    taskInputService.registerEnvelope(envelope)
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

//
//  /**
//    * Register processed envelope in performance metrics.
//    *
//    * @param envelopeID
//    * @param data: processed envelope
//    */
//  private def registerOutputEnvelope(envelopeID: String, data: Envelope) = {
//    val elementSize = byteSerializer.serialize(data).length
//    performanceMetrics.addElementToOutputEnvelope(outputStream.name, envelopeID, elementSize)
//  }


//  /**
//    * Register and send envelope to storage.
//    *
//    * @param outputEnvelope
//    * @param inputEnvelope
//    */
//  private def registerAndSendOutputEnvelope(outputEnvelope: Envelope, inputEnvelope: TStreamEnvelope) = {
//    registerOutputEnvelope(inputEnvelope.id.toString.replaceAll("-", ""), outputEnvelope)
//    storageWrite()
    //    outputEnvelope match {
    //      case esEnvelope: EsEnvelope => writeToES(esEnvelope, inputEnvelope)
    //      case jdbcEnvelope: JdbcEnvelope => writeToJdbc(jdbcEnvelope, inputEnvelope)
    //      case _ =>
    //    }
//  }




// todo
//  /**
//    * Doing smth after catch envelope.
//    */
//  protected def afterReceivingEnvelope() = {}

//  /**
//    *Prepare storage for processed envelopes.
//    */
//  def storagePrepare() = {
//    //todo realize
//  }
//
//  /**
//    * Put processed envelopes to storage.
//    */
//  def storageWrite() = {
//    //todo realize
//  }
//
//  /**
//    * Remove envelopes from storage.
//    */
//  def storageRemove() = {
//    //todo realize
//  }


  /**
    * Does group checkpoint of t-streams consumers/producers
    */
  protected def doCheckpoint() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    prepareForNextCheckpoint()
    wasFirstCheckpoint = true
  }

  protected def prepareForNextCheckpoint(): Unit


//  /**
//   * Open elasticsearch connection
//   *
//   * @param outputStream Output stream
//   * @return ES Transport client and ES service of stream
//   */
//  private def openEsConnection(outputStream: SjStream) = {
//    logger.info(s"Task: ${manager.taskName}. Open output elasticsearch connection.\n")
//    val esService: ESService = outputStream.service.asInstanceOf[ESService]
//    val hosts = esService.provider.hosts.map { host =>
//      val parts = host.split(":")
//      (parts(0), parts(1).toInt)
//    }.toSet
//    val client = new ElasticsearchClient(hosts)
//
//
//    (client, esService)
//  }


//  /**
//   * Open JDBC connection
//   *
//   * @param outputStream Output stream
//   * @return JDBC connection client and JDBC service of stream
//   */
//  private def openJdbcConnection(outputStream: SjStream) = {
//    logger.info(s"Task: ${manager.taskName}. Open output JDBC connection.\n")
//    val jdbcService: JDBCService = outputStream.service.asInstanceOf[JDBCService]
//    val hosts = jdbcService.provider.hosts
//
//    val client = JdbcClientBuilder.
//      setHosts(hosts).
//      setDriver(jdbcService.driver).
//      setUsername(jdbcService.provider.login).
//      setPassword(jdbcService.provider.password).
//      setTable(outputStream.name).
//      setDatabase(jdbcService.database).
//      setTxnField(txnFieldForJdbc).
//      build()
//    (client, jdbcService)
//  }







//  private def prepareES() = {
//    esService.prepare()
//    createIndexMapping()
//  }

//  private def createIndexMapping() = {
//    val index = esService.index
//    logger.debug(s"Task: ${manager.taskName}. Create the mapping for the elasticsearch index $index")
//    val streamName = outputStream.name
//    val entity = manager.getOutputModuleEntity()
//    val fields = scala.collection.mutable.Map("txn" -> Map("type" -> "string"),
//      "stream" -> Map("type" -> "string"),
//      "partition" -> Map("type" -> "integer"))
//    val dateFields: Array[String] = entity.getDateFields()
//    dateFields.foreach { field =>
//      fields.put(field, Map("type" -> "date", "format" -> "epoch_millis"))
//    }
//    val mapping = Map("properties" -> fields)
//    val mappingJson = envelopeSerializer.serialize(mapping)
//
//    esClient.createMapping(index, streamName, mappingJson)
//  }



//  private def removeFromES(envelope: TStreamEnvelope) = {
//    if (!wasFirstCheckpoint) {
//      val transaction = envelope.id.toString.replaceAll("-", "")
//      removeTxnFromES(transaction)
//    }
//  }

//  private def removeTxnFromES(transaction: String) = {
//    val index = esService.index
//    val streamName = outputStream.name
//    logger.info(s"Task: ${manager.taskName}. Delete transaction $transaction from ES stream.")
//    if (esClient.doesIndexExist(index)) {
//      val query = QueryBuilders.matchQuery("txn", transaction)
//      val outputData = esClient.search(index, streamName, query)
//
//      outputData.getHits.foreach { hit =>
//        val id = hit.getId
//        esClient.deleteDocumentByTypeAndId(index, streamName, id)
//      }
//    }
//  }








//  /**
//   * Writing entity to elasticsearch
//   *
//   * @param esEnvelope:
//   * @param inputEnvelope:
//   */
//  private def writeToES(esEnvelope: EsEnvelope, inputEnvelope: TStreamEnvelope) = {
//    esEnvelope.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
//    // todo REMOVED 4 zeros from transactionDateTime (check it)
//    esEnvelope.transactionDateTime = s"${inputEnvelope.id}".dropRight(4)
//    esEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
//    esEnvelope.stream = inputEnvelope.stream
//    esEnvelope.partition = inputEnvelope.partition
//    esEnvelope.tags = inputEnvelope.tags
//
//    logger.debug(s"Task: ${manager.taskName}. Write output envelope to elasticsearch.")
//    esClient.write(envelopeSerializer.serialize(esEnvelope), esService.index, outputStream.name)
//  }



//  private def removeFromJdbc(envelope: TStreamEnvelope): Unit = {
//    if (!wasFirstCheckpoint) {
//      val transaction = envelope.id.toString.replaceAll("-", "")
//      jdbcClient.removeByTransactionId(transaction)
//    }
//  }
//
//  /**
//   * Writing entity to JDBC
//   *
//   * @param jdbcEnvelope: Output envelope for writing to JDBC
//   */
//  private def writeToJdbc(jdbcEnvelope: JdbcEnvelope, inputEnvelope: TStreamEnvelope) = {
//    txnFieldForJdbc = jdbcEnvelope.getTxnName
//    val jdbcStream = outputStream.asInstanceOf[JDBCSjStream]
//    jdbcEnvelope.txn = inputEnvelope.id.toString.replaceAll("-", "")
//    jdbcEnvelope.setV(jdbcStream.primary, UUID.randomUUID().toString)
//    jdbcClient.write(jdbcEnvelope)
//  }



}
