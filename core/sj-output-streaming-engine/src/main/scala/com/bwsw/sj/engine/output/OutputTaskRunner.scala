package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetAddress
import java.util.concurrent.{ArrayBlockingQueue, ExecutorCompletionService, ExecutorService, Executors}
import java.util.{Calendar, UUID}

import com.bwsw.common.traits.Serializer
import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.{TStreamSjStream, ESService, FileMetadata, SjStream}
import com.bwsw.sj.engine.core.entities.{EsEntity, OutputEntity, OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.engine.core.utils.EngineUtils._
import com.bwsw.sj.engine.output.subscriber.OutputSubscriberCallback
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.consumer.subscriber.SubscribingConsumer
import com.datastax.driver.core.utils.UUIDs
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.{Logger, LoggerFactory}

/**
 * Runner object for engine of output-streaming module
 * Created: 26/05/2016
 *
 * @author Kseniya Tomskikh
 */
object OutputTaskRunner {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val threadFactory = createThreadFactory()
  private val threadPool: ExecutorService = Executors.newFixedThreadPool(3, threadFactory)
  private val executorService: ExecutorCompletionService[Unit] = new ExecutorCompletionService[Unit](threadPool)

  private val objectSerializer = new ObjectSerializer()

  val serializer: Serializer = new JsonSerializer

  def main(args: Array[String]) = {

    val instance: OutputInstance = OutputDataFactory.instance
    val taskManager: OutputTaskManager = new OutputTaskManager()
    logger.info(s"Task: ${OutputDataFactory.taskName}. Start preparing of task runner for output module.")

    val inputStream: SjStream = OutputDataFactory.inputStream
    val outputStream: SjStream = OutputDataFactory.outputStream

    val taskPartitions: Array[Int] = taskManager.task.get(inputStream.name).get
    val blockingQueue: ArrayBlockingQueue[String] = new ArrayBlockingQueue[String](1000)

    logger.debug(s"Task: ${OutputDataFactory.taskName}. Start creating subscribing consumer.")

    val callback = new OutputSubscriberCallback(blockingQueue)

    val subscribeConsumer = taskManager.createSubscribingConsumer(
      inputStream.asInstanceOf[TStreamSjStream],
      taskPartitions.toList,
      chooseOffset(instance.startFrom),
      callback
    )
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Creation of subscribing consumer is finished.")

    logger.debug(s"Task: ${OutputDataFactory.taskName}. Start loading of executor (handler) class from module jar.")
    val moduleJar: File = OutputDataFactory.getModuleJar
    val moduleMetadata: FileMetadata = OutputDataFactory.getFileMetadata
    val handler: OutputStreamingHandler = taskManager.getExecutor.asInstanceOf[OutputStreamingHandler]

    val entity: OutputEntity = taskManager.getOutputModuleEntity(moduleJar, moduleMetadata.specification.entityClass)

    val performanceMetrics = new OutputStreamingPerformanceMetrics(taskManager)

    logger.debug(s"Task: ${OutputDataFactory.taskName}. Launch a new thread to report performance metrics \n")
    executorService.submit(performanceMetrics)

    logger.info(s"Task: ${OutputDataFactory.taskName}. Preparing finished. Launch task.")
    try {
      runModule(instance,
        blockingQueue,
        subscribeConsumer,
        taskManager,
        handler,
        entity,
        outputStream,
        performanceMetrics)
    } catch {
      case e: Exception =>
        logger.error(e.getStackTrace.toString)
        threadPool.shutdownNow()
        System.exit(-1)
    }
  }

  def createThreadFactory() = {
    new ThreadFactoryBuilder()
      .setNameFormat("RegularTaskRunner-%d")
      .build()
  }
  /**
   *
   * @param instance Instance of output streaming module
   * @param blockingQueue Queue with transactions from t-stream
   * @param subscribeConsumer Subscribe consumer for read messages from t-stream
   * @param taskManager Task manager for control task of this module
   * @param handler User handler (executor) of output-streaming module
   * @param outputStream Output stream for transform data
   */
  def runModule(instance: OutputInstance,
                blockingQueue: ArrayBlockingQueue[String],
                subscribeConsumer: SubscribingConsumer[Array[Byte]],
                taskManager: OutputTaskManager,
                handler: OutputStreamingHandler,
                outputModuleEntity: OutputEntity,
                outputStream: SjStream,
                performanceMetrics: OutputStreamingPerformanceMetrics) = {
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Launch subscribing consumer.")
    subscribeConsumer.start()

    val (client, esService) = openDbConnection(outputStream)
    createEsStream(esService.index, outputStream.name, outputModuleEntity, client)

    instance.checkpointMode match {
      //todo пока так
      case "every-nth" =>
        var countOfTxn = 0
        var isFirstCheckpoint = false
        while (true) {
          logger.debug(s"Task: ${OutputDataFactory.taskName}. Start a output module with every-nth checkpoint mode.")

          val nextEnvelope: String = blockingQueue.take()

          if (nextEnvelope != null && !nextEnvelope.equals("")) {
            logger.info(s"Task: ${OutputDataFactory.taskName}. Get t-stream envelope.")
            countOfTxn += 1
            logger.debug(s"Task: ${OutputDataFactory.taskName}. Envelope processing...")
            val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](nextEnvelope)
            subscribeConsumer.setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)

            if (!isFirstCheckpoint) {
              deleteTransactionFromES(tStreamEnvelope.txnUUID.toString.replaceAll("-", ""), esService.index, outputStream.name, client)
            }

            performanceMetrics.addEnvelopeToInputStream(
              tStreamEnvelope.stream,
              tStreamEnvelope.data.map(_.length)
            )
            val outputEnvelopes: List[OutputEnvelope] = handler.onTransaction(tStreamEnvelope)
            outputEnvelopes.foreach { (outputEnvelope: OutputEnvelope) =>
              performanceMetrics.addElementToOutputEnvelope(
                outputStream.name,
                tStreamEnvelope.txnUUID.toString,
                objectSerializer.serialize(outputEnvelope.data).length
              )
              outputEnvelope.streamType match {
                case "elasticsearch-output" =>
                  val entity = outputEnvelope.data.asInstanceOf[EsEntity]
                  entity.outputDateTime = s"${Calendar.getInstance().getTimeInMillis}"
                  entity.txnDateTime = s"${UUIDs.unixTimestamp(tStreamEnvelope.txnUUID)}"
                  entity.txn = tStreamEnvelope.txnUUID.toString.replaceAll("-", "")
                  entity.stream = tStreamEnvelope.stream
                  entity.partition = tStreamEnvelope.partition
                  writeToElasticsearch(esService.index, outputStream.name, entity, client)
                case "jdbc-output" => writeToJdbc(outputEnvelope)
                case _ =>
              }
            }
          }

          if (countOfTxn == instance.checkpointInterval) {
            logger.debug(s"Task: ${OutputDataFactory.taskName}. Checkpoint.")
            subscribeConsumer.checkpoint()
            countOfTxn = 0
            isFirstCheckpoint = true
          }
        }
    }

  }

  /**
   * Delete transactions from ES stream
   * from last checkpoint
   *
   * @param txn Transaction UUID
   * @param index ES index
   * @param documentType ES Stream name
   * @param client ES Transport client
   */
  def deleteTransactionFromES(txn: String, index: String, documentType: String, client: TransportClient) = {
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

  /**
   * Open elasticsearch connection
   *
   * @param outputStream Output ES stream
   * @return ES Transport client and ES service of stream
   */
  def openDbConnection(outputStream: SjStream): (TransportClient, ESService) = {
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
   * Writing entity to ES
   *
   * @param index ES index
   * @param documentType ES document type (name of stream)
   * @param entity ES entity (data row)
   * @param client ES Transport client
   * @return Response from ES
   */
  def writeToElasticsearch(index: String, documentType: String, entity: EsEntity, client: TransportClient) = {
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Write output envelope to elasticearch.")
    val esData: String = serializer.serialize(entity)

    val request: IndexRequestBuilder = client.prepareIndex(index, documentType, UUID.randomUUID().toString)
    request.setSource(esData)
    request.execute().actionGet()

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
  def createEsStream(index: String, streamName: String, entity: OutputEntity, client: TransportClient) = {
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

  /**
   * Writing entity to JDBC
   *
   * @param envelope Output envelope for writing to sql database
   */
  def writeToJdbc(envelope: OutputEnvelope) = {
    //todo
  }

}

