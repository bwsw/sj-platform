package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.stream.SjStream
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Provides methods to access [[SjStream]]s in [[GenericMongoRepository]]
  */
class StreamSI extends ServiceInterface[SjStream, StreamDomain] {
  override protected val entityRepository: GenericMongoRepository[StreamDomain] = ConnectionRepository.getStreamRepository

  private val instanceRepository = ConnectionRepository.getInstanceRepository

  override def create(entity: SjStream): Either[ArrayBuffer[String], Boolean] = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      entity.create()
      entityRepository.save(entity.to())
      Right(true)
    } else {
      Left(errors)
    }
  }

  override def get(name: String): Option[SjStream] =
    entityRepository.get(name).map(SjStream.from)

  override def getAll(): mutable.Buffer[SjStream] =
    entityRepository.getAll.map(SjStream.from)

  override def delete(name: String): Either[String, Boolean] = {
    if (hasRelatedInstances(name))
      Left(createMessage("rest.streams.stream.cannot.delete", name))
    else entityRepository.get(name) match {
      case Some(entity) =>
        SjStream.from(entity).delete()
        entityRepository.delete(name)
        Right(true)
      case None =>
        Right(false)
    }
  }

  /**
    * Returns [[com.bwsw.sj.common.dal.model.instance.InstanceDomain InstanceDomain]]s related with [[SjStream]]
    *
    * @param name name of stream
    * @return Some(instances) if stream exists, None otherwise
    */
  def getRelated(name: String): Option[mutable.Buffer[String]] =
    entityRepository.get(name).map(_ => getRelatedInstances(name))

  private def getRelatedInstances(streamName: String): mutable.Buffer[String] =
    getAllInstances.filter(related(streamName)).map(_.name)

  private def related(streamName: String)(instance: Instance): Boolean =
    instance.streams.contains(streamName)

  private def hasRelatedInstances(streamName: String): Boolean =
    getAllInstances.exists(related(streamName))

  private def getAllInstances =
    instanceRepository.getAll.map(Instance.from)
}
