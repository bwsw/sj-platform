package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.service.Service
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Provides methods to access [[Service]]s in [[GenericMongoRepository]]
  */
class ServiceSI extends ServiceInterface[Service, ServiceDomain] {
  override protected val entityRepository: GenericMongoRepository[ServiceDomain] = ConnectionRepository.getServiceRepository

  private val streamRepository = ConnectionRepository.getStreamRepository
  private val instanceRepository = ConnectionRepository.getInstanceRepository

  override def create(entity: Service): Either[ArrayBuffer[String], Boolean] = {
    val errors = new ArrayBuffer[String]

    errors ++= entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity.to())

      Right(true)
    } else {
      Left(errors)
    }
  }

  def getAll(): mutable.Buffer[Service] = {
    entityRepository.getAll.map(x => Service.from(x))
  }

  def get(name: String): Option[Service] = {
    entityRepository.get(name).map(Service.from)
  }

  override def delete(name: String): Either[String, Boolean] = {
    var response: Either[String, Boolean] = Left(createMessage("rest.services.service.cannot.delete.due.to.streams", name))
    val streams = getRelatedStreams(name)

    if (streams.isEmpty) {
      response = Left(createMessage("rest.services.service.cannot.delete.due.to.instances", name))
      val instances = getRelatedInstances(name)

      if (instances.isEmpty) {
        val provider = entityRepository.get(name)

        provider match {
          case Some(_) =>
            entityRepository.delete(name)
            response = Right(true)
          case None =>
            response = Right(false)
        }
      }
    }

    response
  }

  /**
    * Returns [[com.bwsw.sj.common.si.model.stream.SjStream SjStream]]s and
    * [[com.bwsw.sj.common.dal.model.instance.InstanceDomain InstanceDomain]]s related with [[Service]]
    *
    * @param name name of service
    * @return Right((streams, instances)) if service exists, Left(false) otherwise
    */
  def getRelated(name: String): Either[Boolean, (mutable.Buffer[String], mutable.Buffer[String])] = {
    val service = entityRepository.get(name)

    service match {
      case Some(_) => Right((getRelatedStreams(name), getRelatedInstances(name)))
      case None => Left(false)
    }
  }

  private def getRelatedStreams(serviceName: String): mutable.Buffer[String] = {
    streamRepository.getAll.filter(
      s => s.service.name.equals(serviceName)
    ).map(_.name)
  }

  private def getRelatedInstances(serviceName: String): mutable.Buffer[String] = {
    instanceRepository.getAll.filter(
      s => s.coordinationService.name.equals(serviceName)
    ).map(_.name)
  }
}
