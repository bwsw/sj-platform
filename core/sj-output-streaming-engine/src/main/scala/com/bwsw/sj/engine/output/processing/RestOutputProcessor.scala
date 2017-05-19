package com.bwsw.sj.engine.output.processing

import com.bwsw.common.JsonSerializer
import com.bwsw.common.rest.RestClient
import com.bwsw.sj.common.dal.model.stream.RestStreamDomain
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.output.task.OutputTaskManager
import com.bwsw.sj.engine.output.task.reporting.OutputStreamingPerformanceMetrics
import org.apache.http.entity.ContentType

import scala.collection.JavaConverters._

/**
  * ref. [[OutputProcessor]] object
  * @author Pavel Tomskikh
  */
class RestOutputProcessor[T <: AnyRef](restOutputStream: RestStreamDomain,
                                       performanceMetrics: OutputStreamingPerformanceMetrics,
                                       manager: OutputTaskManager,
                                       entity: Entity[_])
  extends OutputProcessor[T](restOutputStream, performanceMetrics) {

  private val jsonSerializer = new JsonSerializer
  private val service = restOutputStream.service
  private val client = new RestClient(
    service.provider.hosts.toSet,
    service.basePath + "/" + restOutputStream.name,
    service.httpVersion,
    Map(service.headers.asScala.toList: _*),
    Option(service.provider.name),
    Option(service.provider.password)
  )

  override def send(envelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[T]): Unit = {
    logger.debug(createLogMessage("Write an output envelope to RESTful stream."))
    val entity = envelope.getFieldsValue + (transactionFieldName -> inputEnvelope.id)
    val data = jsonSerializer.serialize(entity)
    val posted = client.post(data, ContentType.APPLICATION_JSON.toString)

    if (!posted) {
      val errorMessage = createLogMessage(s"Cannot send envelope '${inputEnvelope.id}'.")
      logger.error(errorMessage)
      delete(inputEnvelope)
      throw new RuntimeException(errorMessage)
    }
  }

  override def delete(envelope: TStreamEnvelope[T]): Unit = {
    logger.debug(createLogMessage(s"Delete a transaction: '${envelope.id}' from RESTful stream."))
    val deleted = client.delete(transactionFieldName, envelope.id.toString)
    if (!deleted)
      logger.warn(createLogMessage(s"Transaction '${envelope.id}' not deleted."))
  }

  override def close(): Unit = client.close()

  private def createLogMessage(message: String) = s"Task: '${manager.taskName}'. $message"
}
