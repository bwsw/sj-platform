package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.utils.EngineUtils._
import com.bwsw.sj.common.DAL.model.{ESService, FileMetadata, SjStream}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.engine.core.entities.{EsEntity, OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

/**
  * Runner object for engine of output-streaming module
  * Created: 26/05/2016
  *
  * @author Kseniya Tomskikh
  */
object OutputTaskRunner {
  val serializer: Serializer = new JsonSerializer

  def main(args: Array[String]) = {

    val instance: OutputInstance = OutputDataFactory.instance
    val taskManager: OutputTaskManager = new OutputTaskManager(OutputDataFactory.taskName, instance)

    val inputStream: SjStream = OutputDataFactory.inputStream
    val outputStream: SjStream = OutputDataFactory.outputStream

    val taskPartitions: Array[Int] = taskManager.task.get(inputStream.name).get
    val blockingQueue: ArrayBlockingQueue[String] = new ArrayBlockingQueue[String](1000)
    val subscribeConsumer = taskManager.createSubscribingConsumer(
      inputStream,
      taskPartitions.toList,
      chooseOffset(instance.startFrom),
      blockingQueue
    )
    val moduleJar: File = OutputDataFactory.getModuleJar
    val moduleMetadata: FileMetadata = OutputDataFactory.getFileMetadata
    val handler: OutputStreamingHandler = taskManager.getModuleHandler(moduleJar, moduleMetadata.specification.executorClass)

    runModule(instance,
      blockingQueue,
      subscribeConsumer,
      taskManager,
      handler,
      outputStream)

  }

  /**
    *
    * @param instance Instance of output streaming module
    * @param blockingQueue Queue with transactions from t-stream
    * @param subscribeConsumer Subscribe consumer for read messages from t-stream
    * @param taskManager Task manager for control task of this module
    * @param handler User handler (executor) of output-streaming module
    * @param outputStream Output stream for transorm data
    */
  def runModule(instance: OutputInstance,
                blockingQueue: ArrayBlockingQueue[String],
                subscribeConsumer: BasicSubscribingConsumer[Array[Byte], Array[Byte]],
                taskManager: OutputTaskManager,
                handler: OutputStreamingHandler,
                outputStream: SjStream) = {

    subscribeConsumer.start()

    val (client, esService) = openDbConnection(outputStream)

    instance.checkpointMode match {
      case "time-interval" =>
        val checkpointTimer = new SjTimer()
        checkpointTimer.set(instance.checkpointInterval)
        while(true) {
          processTransaction(blockingQueue, subscribeConsumer, handler, outputStream, client, esService)
          if (checkpointTimer.isTime) {
            subscribeConsumer.checkpoint()
            checkpointTimer.reset()
            checkpointTimer.set(instance.checkpointInterval)
          }
        }
      case "every-nth" =>
        var countOfTxn = 0
        while (true) {
          processTransaction(blockingQueue, subscribeConsumer, handler, outputStream, client, esService)
          if (countOfTxn == instance.checkpointInterval) {
            subscribeConsumer.checkpoint()
            countOfTxn = 0
          } else {
            countOfTxn += 1
          }
        }
    }


  }

  /**
    * Read and transform transaction
    * writin to output datasource
    *
    * @param queue Queue with t-stream transactions
    * @param subscribeConsumer Subscribe consumer for read messages from t-stream
    * @param handler User handler (executor) of output-streaming module
    * @param outputStream Output stream
    * @param client elasticsearch transport client
    * @param esService elasticsearch service of output stream
    */
  def processTransaction(queue: ArrayBlockingQueue[String],
                         subscribeConsumer: BasicSubscribingConsumer[Array[Byte], Array[Byte]],
                         handler: OutputStreamingHandler,
                         outputStream: SjStream,
                         client: TransportClient,
                         esService: ESService) = {

    val nextEnvelope: String = queue.take()
    if (nextEnvelope != null && !nextEnvelope.equals("")) {
      val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](nextEnvelope)
      subscribeConsumer.setLocalOffset(tStreamEnvelope.partition, tStreamEnvelope.txnUUID)
      val outputEnvelopes: List[OutputEnvelope] = handler.onTransaction(tStreamEnvelope)
      outputEnvelopes.foreach { (outputEnvelope: OutputEnvelope) =>
        outputEnvelope.streamType match {
          case "elasticsearch-output" =>
            val entity = outputEnvelope.data.asInstanceOf[EsEntity]
            writeToElasticsearch(esService.index, outputStream.name, entity, client)
          case "jdbc-output" => writeToJdbc(outputEnvelope)
          case _ =>
        }
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

    val request: IndexRequestBuilder = client.prepareIndex(index, documentType)
    request.setSource(esData)
    request.execute().actionGet()
  }

  def writeToJdbc(envelope: OutputEnvelope) = {

  }

}
