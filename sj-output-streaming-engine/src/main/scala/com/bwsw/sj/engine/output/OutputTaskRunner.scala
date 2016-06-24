package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetAddress
import java.util.UUID

import java.util.concurrent.{ArrayBlockingQueue, Executors}

import com.bwsw.common.{ObjectSerializer, JsonSerializer}
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.DAL.model.{ESService, FileMetadata, SjStream}
import com.bwsw.sj.common.module.{OutputStreamingPerformanceMetrics}
import com.bwsw.sj.engine.core.entities.{EsEntity, OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.engine.core.utils.EngineUtils._
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import com.bwsw.tstreams.agents.producer.{ProducerPolicies, BasicProducerTransaction}
import com.datastax.driver.core.utils.UUIDs
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
  private val executorService = Executors.newCachedThreadPool()
  private val objectSerializer = new ObjectSerializer()

  val serializer: Serializer = new JsonSerializer

  def main(args: Array[String]) = {

    val instance: OutputInstance = OutputDataFactory.instance
    val taskManager: OutputTaskManager = new OutputTaskManager(OutputDataFactory.taskName, instance)
    logger.info(s"Task: ${OutputDataFactory.taskName}. Start preparing of task runner for output module.")

    val inputStream: SjStream = OutputDataFactory.inputStream
    val outputStream: SjStream = OutputDataFactory.outputStream

    val taskPartitions: Array[Int] = taskManager.task.get(inputStream.name).get
    val blockingQueue: ArrayBlockingQueue[String] = new ArrayBlockingQueue[String](1000)

    logger.debug(s"Task: ${OutputDataFactory.taskName}. Start creating subscribing consumer.")
    val subscribeConsumer = taskManager.createSubscribingConsumer(
      inputStream,
      taskPartitions.toList,
      chooseOffset(instance.startFrom),
      blockingQueue
    )
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Creation of subscribing consumer is finished.")

    logger.debug(s"Task: ${OutputDataFactory.taskName}. Start loading of executor (handler) class from module jar.")
    val moduleJar: File = OutputDataFactory.getModuleJar
    val moduleMetadata: FileMetadata = OutputDataFactory.getFileMetadata
    val handler: OutputStreamingHandler = taskManager.getModuleHandler(moduleJar, moduleMetadata.specification.executorClass)

    logger.debug(s"Task: ${OutputDataFactory.taskName}. Start creating a t-stream producer to record performance reports\n")
    val reportStream = OutputDataFactory.getReportStream
    val reportProducer = taskManager.createProducer(reportStream)
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Creation of t-stream producer is finished\n")

    val performanceMetrics: OutputStreamingPerformanceMetrics = new OutputStreamingPerformanceMetrics(
      OutputDataFactory.taskName,
      OutputDataFactory.agentHost,
      inputStream.name,
      outputStream.name
    )

    logger.debug(s"Task: ${OutputDataFactory.taskName}. Launch a new thread to report performance metrics \n")
    executorService.execute(new Runnable() {
      def run() = {
        val taskNumber = OutputDataFactory.taskName.replace(s"${OutputDataFactory.instanceName}-task", "").toInt
        var report: String = null
        var reportTxn: BasicProducerTransaction[Array[Byte], Array[Byte]] = null
        while (true) {
          logger.info(s"Task: ${OutputDataFactory.taskName}. Wait ${instance.performanceReportingInterval} ms to report performance metrics\n")
          Thread.sleep(instance.performanceReportingInterval)
          report = performanceMetrics.getReport
          logger.info(s"Task: ${OutputDataFactory.taskName}. Performance metrics: $report \n")
          logger.debug(s"Task: ${OutputDataFactory.taskName}. Create a new txn for sending performance metrics\n")
          reportTxn = reportProducer.newTransaction(ProducerPolicies.errorIfOpen, taskNumber)
          logger.debug(s"Task: ${OutputDataFactory.taskName}. Send performance metrics\n")
          reportTxn.send(report.getBytes)
          logger.debug(s"Task: ${OutputDataFactory.taskName}. Do checkpoint of producer for performance reporting\n")
          reportProducer.checkpoint()
        }
      }
    })

    logger.info(s"Task: ${OutputDataFactory.taskName}. Preparing finished. Launch task.")
    try {
      runModule(instance,
        blockingQueue,
        subscribeConsumer,
        taskManager,
        handler,
        outputStream,
        performanceMetrics)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        executorService.shutdownNow()
        System.exit(-1)
    }

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
                subscribeConsumer: BasicSubscribingConsumer[Array[Byte], Array[Byte]],
                taskManager: OutputTaskManager,
                handler: OutputStreamingHandler,
                outputStream: SjStream,
                performanceMetrics: OutputStreamingPerformanceMetrics) = {
    logger.debug(s"Task: ${OutputDataFactory.taskName}. Launch subscribing consumer.")
    subscribeConsumer.start()

    val (client, esService) = openDbConnection(outputStream)

    instance.checkpointMode match {
      //todo пока так
      case "every-nth" =>
        var countOfTxn = 0
        var isFirstCheckpoint = false
        while (true) {
          logger.debug(s"Task: ${OutputDataFactory.taskName}. Start a output module with every-nth checkpoint mode.")

          logger.info(s"Task: ${OutputDataFactory.taskName}. Get t-stream envelope.")
          val nextEnvelope: String = blockingQueue.take()

          if (nextEnvelope != null && !nextEnvelope.equals("")) {
            println("envelope processing...")
            val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](nextEnvelope)
            subscribeConsumer.setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)

            if (!isFirstCheckpoint) {
              deleteTransactionFromES(tStreamEnvelope.txnUUID, esService.index, outputStream.name, client)
            }

            performanceMetrics.addEnvelopeToInputStream(
              tStreamEnvelope.stream,
              tStreamEnvelope.data.map(_.length)
            )
            val outputEnvelopes: List[OutputEnvelope] = handler.onTransaction(tStreamEnvelope)
            outputEnvelopes.foreach { (outputEnvelope: OutputEnvelope) =>
              performanceMetrics.addElementToOutputEnvelope(
                outputStream.name,
                outputEnvelope.data.txn.toString,
                objectSerializer.serialize(outputEnvelope.data).length
              )
              outputEnvelope.streamType match {
                case "elasticsearch-output" =>
                  val entity = outputEnvelope.data.asInstanceOf[EsEntity]
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
          } else {
            countOfTxn += 1
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
  def deleteTransactionFromES(txn: UUID, index: String, documentType: String, client: TransportClient) = {
    logger.info(s"Task: ${OutputDataFactory.taskName}. Delete transaction $txn from ES stream.")

    val isIndexExist = client.admin().indices().prepareExists(index).execute().actionGet()
    if (isIndexExist.isExists) {

      val txnTmstp = UUIDs.unixTimestamp(txn)
      val request: SearchRequestBuilder = client
        .prepareSearch(index)
        .setTypes(documentType)
        .setQuery(QueryBuilders.matchQuery("txn", txnTmstp))
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
  def writeToJdbc(envelope: OutputEnvelope) = {
    //todo
  }

}

