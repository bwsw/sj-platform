package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service._
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.provider.Provider
import com.bwsw.sj.common.utils.MessageResourceUtils._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ProviderSI extends ServiceInterface[Provider, ProviderDomain] {
  override protected val entityRepository: GenericMongoRepository[ProviderDomain] = ConnectionRepository.getProviderRepository

  private val serviceRepository = ConnectionRepository.getServiceRepository

  override def create(entity: Provider): Either[ArrayBuffer[String], Boolean] = {
    val errors = new ArrayBuffer[String]

    errors ++= entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity.to())

      Right(true)
    } else {
      Left(errors)
    }
  }

  def getAll(): mutable.Buffer[Provider] = {
    entityRepository.getAll.map(x => Provider.from(x))
  }

  def get(name: String): Option[Provider] = {
    entityRepository.get(name).map(Provider.from)
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
    serviceRepository.getAll.filter {
      case esService: ESServiceDomain =>
        esService.provider.name.equals(providerName)
      case zkService: ZKServiceDomain =>
        zkService.provider.name.equals(providerName)
      case aeroService: AerospikeServiceDomain =>
        aeroService.provider.name.equals(providerName)
      case cassService: CassandraServiceDomain =>
        cassService.provider.name.equals(providerName)
      case kfkService: KafkaServiceDomain =>
        kfkService.provider.name.equals(providerName) || kfkService.zkProvider.name.equals(providerName)
      case tService: TStreamServiceDomain =>
        tService.provider.name.equals(providerName)
      case jdbcService: JDBCServiceDomain =>
        jdbcService.provider.name.equals(providerName)
      case restService: RestServiceDomain =>
        restService.provider.name.equals(providerName)
    }.map(_.name)
  }
}
