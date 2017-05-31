package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service._
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.provider.{Provider, ProviderConversion}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Provides methods to access [[Provider]]s in [[GenericMongoRepository]]
  */
class ProviderSI(implicit injector: Injector) extends ServiceInterface[Provider, ProviderDomain] {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  private val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  override protected val entityRepository: GenericMongoRepository[ProviderDomain] = connectionRepository.getProviderRepository

  private val serviceRepository = connectionRepository.getServiceRepository
  private val providerConversion = inject[ProviderConversion]

  override def create(entity: Provider): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity.to())

      Created
    } else {
      NotCreated(errors)
    }
  }

  def getAll(): mutable.Buffer[Provider] = {
    entityRepository.getAll.map(x => providerConversion.from(x))
  }

  def get(name: String): Option[Provider] = {
    entityRepository.get(name).map(providerConversion.from)
  }

  override def delete(name: String): DeletionResult = {
    if (getRelatedServices(name).nonEmpty)
      DeletionError(createMessage("rest.providers.provider.cannot.delete", name))
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
    * Establishes connection to [[Provider.hosts]]
    *
    * @param name name of provider
    * @return Right(true) if connection established, Right(false) if provider not found in [[entityRepository]],
    *         Left(errors) if some errors happened
    */
  def checkConnection(name: String): Either[ArrayBuffer[String], Boolean] = {
    val provider = entityRepository.get(name)

    provider match {
      case Some(x) =>
        val errors = x.checkConnection()
        if (errors.isEmpty)
          Right(true)
        else
          Left(errors)
      case None => Right(false)
    }
  }

  /**
    * Returns [[com.bwsw.sj.common.si.model.service.Service Service]]s related with [[Provider]]
    *
    * @param name name of provider
    * @return Right(services) if provider exists, Left(false) otherwise
    */
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
