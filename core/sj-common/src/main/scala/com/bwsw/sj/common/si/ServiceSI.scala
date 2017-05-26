package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.service.Service
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable

/**
  * Provides methods to access [[Service]]s in [[GenericMongoRepository]]
  */
class ServiceSI extends ServiceInterface[Service, ServiceDomain] {
  override protected val entityRepository: GenericMongoRepository[ServiceDomain] = ConnectionRepository.getServiceRepository

  private val streamRepository = ConnectionRepository.getStreamRepository
  private val instanceRepository = ConnectionRepository.getInstanceRepository

  override def create(entity: Service): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity.to())

      Created
    } else {
      NotCreated(errors)
    }
  }

  def getAll(): mutable.Buffer[Service] = {
    entityRepository.getAll.map(x => Service.from(x))
  }

  def get(name: String): Option[Service] = {
    entityRepository.get(name).map(Service.from)
  }

  override def delete(name: String): DeletingResult = {
    if (getRelatedStreams(name).nonEmpty)
      DeletingError(createMessage("rest.services.service.cannot.delete.due.to.streams", name))
    else if (getRelatedInstances(name).nonEmpty)
      DeletingError(createMessage("rest.services.service.cannot.delete.due.to.instances", name))
    else {
      entityRepository.get(name) match {
        case Some(_) =>
          entityRepository.delete(name)
          Deleted
        case None =>
          EntityNotFound
      }
    }
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
