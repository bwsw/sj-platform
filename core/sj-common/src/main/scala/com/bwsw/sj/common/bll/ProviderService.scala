package com.bwsw.sj.common.bll

import com.bwsw.sj.common.dal.model.provider.Provider
import com.bwsw.sj.common.dal.model.service._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.dal.service.GenericMongoRepository
import com.bwsw.sj.common.utils.MessageResourceUtils._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ProviderService extends Service[Provider] {
  override protected val entityRepository: GenericMongoRepository[Provider] = ConnectionRepository.getProviderService

  private def serviceDAO = ConnectionRepository.getServiceManager

  override def process(entity: Provider): Either[ArrayBuffer[String], Boolean] = {
    val errors = new ArrayBuffer[String]

    errors ++= entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity)

      Right(true)
    } else {
      Left(errors)
    }
  }

  override def getAll(): mutable.Buffer[Provider] = {
    entityRepository.getAll
  }

  override def get(name: String): Option[Provider] = {
    entityRepository.get(name)
  }

  override def delete(name: String): Either[String, Boolean] = {
    var response: Either[String, Boolean] = Left(createMessage("rest.providers.provider.cannot.delete", name))
    val services = getRelatedServices(name)

    if (services.isEmpty) {
      val provider = entityRepository.get(name)

      provider match {
        case Some(_) =>
          entityRepository.delete(name)
          response = Right(true)
        case None =>
          response = Right(false)
      }
    }

    response
  }

  def checkConnection(name: String): Either[ArrayBuffer[String], Boolean] = {
    val provider = entityRepository.get(name)

    provider match {
      case Some(x) =>
        val errors = x.checkConnection()
        if (errors.isEmpty) {
          Right(true)
        }
        else {
          Left(errors)
        }
      case None => Right(false)
    }
  }

  def getRelated(name: String): Either[Boolean, mutable.Buffer[String]] = {
    val provider = entityRepository.get(name)

    provider match {
      case Some(_) => Right(getRelatedServices(name))
      case None => Left(false)
    }
  }

  private def getRelatedServices(providerName: String): mutable.Buffer[String] = {
    serviceDAO.getAll.filter {
      case esService: ESService =>
        esService.provider.name.equals(providerName)
      case zkService: ZKService =>
        zkService.provider.name.equals(providerName)
      case aeroService: AerospikeService =>
        aeroService.provider.name.equals(providerName)
      case cassService: CassandraService =>
        cassService.provider.name.equals(providerName)
      case kfkService: KafkaService =>
        kfkService.provider.name.equals(providerName) || kfkService.zkProvider.name.equals(providerName)
      case tService: TStreamService =>
        tService.provider.name.equals(providerName)
      case jdbcService: JDBCService =>
        jdbcService.provider.name.equals(providerName)
      case restService: RestService =>
        restService.provider.name.equals(providerName)
    }.map(_.name)
  }
}
