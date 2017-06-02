package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.service.{Service, ServiceConversion}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable

/**
  * Provides methods to access [[Service]]s in [[GenericMongoRepository]]
  */
class ServiceSI(implicit injector: Injector) extends ServiceInterface[Service, ServiceDomain] {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  private val connectionRepository = inject[ConnectionRepository]

  override protected val entityRepository: GenericMongoRepository[ServiceDomain] = connectionRepository.getServiceRepository

  private val streamRepository = connectionRepository.getStreamRepository
  private val instanceRepository = connectionRepository.getInstanceRepository
  private val serviceConversion = inject[ServiceConversion]

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
    entityRepository.getAll.map(x => serviceConversion.from(x))
  }

  def get(name: String): Option[Service] = {
    entityRepository.get(name).map(serviceConversion.from)
  }

  override def delete(name: String): DeletionResult = {
    if (getRelatedStreams(name).nonEmpty)
      DeletionError(createMessage("rest.services.service.cannot.delete.due.to.streams", name))
    else {
      if (getRelatedInstances(name).nonEmpty)
        DeletionError(createMessage("rest.services.service.cannot.delete.due.to.instances", name))
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
