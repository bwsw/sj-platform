package com.bwsw.sj.engine.output

import java.io.File
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.JsonSerializer
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.engine.core.utils.EngineUtils._
import com.bwsw.sj.common.DAL.model.{ESService, FileMetadata, SjStream}
import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.engine.core.entities.{EsEntity, OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.tstreams.agents.consumer.subscriber.BasicSubscribingConsumer
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created: 26/05/2016
  *
  * @author Kseniya Tomskikh
  */
object OutputTaskRunner {
  val serializer: Serializer = new JsonSerializer

  def main(args: Array[String]) = {

    val taskManager: OutputTaskManager = new OutputTaskManager
    val instance: OutputInstance = taskManager.getOutputInstance
    val task: mutable.Map[String, Array[Int]] = instance.executionPlan.tasks.get(taskManager.taskName).inputs.asScala
    val inputStream: SjStream = taskManager.getInputStream(instance)
    val outputStream: SjStream = taskManager.getOutputStream(instance)

    val taskPartitions: Array[Int] = task.get(inputStream.name).get
    val blockingQueue: ArrayBlockingQueue[String] = new ArrayBlockingQueue[String](1000)
    val subscribeConsumer = taskManager.createSubscribingConsumer(
      inputStream,
      taskPartitions.toList,
      chooseOffset(instance.startFrom),
      blockingQueue
    )
    val moduleJar: File = taskManager.getModuleJar(instance)
    val moduleMetadata: FileMetadata = taskManager.getFileMetadata(instance)
    val handler: OutputStreamingHandler = taskManager.getModuleHandler(moduleJar, moduleMetadata.specification.executorClass)

    runModule(instance,
      blockingQueue,
      subscribeConsumer,
      taskManager,
      handler,
      outputStream)

  }

  def runModule(instance: OutputInstance,
                persistentQueue: ArrayBlockingQueue[String],
                subscribeConsumer: BasicSubscribingConsumer[Array[Byte], Array[Byte]],
                taskManager: OutputTaskManager,
                handler: OutputStreamingHandler,
                outputStream: SjStream) = {

    subscribeConsumer.start()

    val esService: ESService = outputStream.service.asInstanceOf[ESService]

    val client: TransportClient = TransportClient.builder().build()
    esService.provider.hosts.foreach { host =>
      val parts = host.split(":")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(parts(0)), parts(1).toInt))
    }

    while(true) {
      val tStreamEnvelope = serializer.deserialize[TStreamEnvelope](persistentQueue.take())

      val outputEnvelopes: List[OutputEnvelope] = handler.onTransaction(tStreamEnvelope)
      val entities = outputEnvelopes.map { (outputEnvelope: OutputEnvelope) =>
        outputEnvelope.data.asInstanceOf[EsEntity]
      }.foreach(entity => writeToElasticsearch(esService.index, outputStream.name, entity, client))


    }

  }

  def writeToElasticsearch(index: String, documentType: String, entity: EsEntity, client: TransportClient) = {
    val esData: String = serializer.serialize(entity)

    val request: IndexRequestBuilder = client.prepareIndex(index, documentType)
    request.setSource(esData)
    request.execute().actionGet()

  }

}
