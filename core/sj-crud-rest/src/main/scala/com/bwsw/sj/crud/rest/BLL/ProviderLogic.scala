package com.bwsw.sj.crud.rest.BLL

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.model.service._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.provider.ProviderData
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ProviderLogic extends Logic[Provider] {
  override protected val entityDAO: GenericMongoService[Provider] = ConnectionRepository.getProviderService
  private def serviceDAO = ConnectionRepository.getServiceManager

  override def process(serializedEntity: String): RestResponse = {
    val errors = new ArrayBuffer[String]
    var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
      createMessage("rest.providers.provider.cannot.create", errors.mkString(";"))))

    try {
      val data = serializer.deserialize[ProviderData](serializedEntity)
      errors ++= data.validate()

      if (errors.isEmpty) {
        entityDAO.save(data.asModelProvider())
        response = CreatedRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.created", data.name)))
      }
    } catch {
      case e: JsonDeserializationException =>
        errors += JsonDeserializationErrorMessageCreator(e)
    }

    if (errors.nonEmpty) {
      response = BadRequestRestResponse(MessageResponseEntity(
        createMessage("rest.providers.provider.cannot.create", errors.mkString(";"))
      ))
    }

    response
  }

  override def getAll(): RestResponse = {
    val providers = entityDAO.getAll
    val response = OkRestResponse(ProvidersResponseEntity())
    if (providers.nonEmpty) {
      response.entity = ProvidersResponseEntity(providers.map(p => p.asProtocolProvider()))
    }

    response
  }

  override def get(name: String): RestResponse = {
    val provider = entityDAO.get(name)
    var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
      createMessage("rest.providers.provider.notfound", name)))

    provider match {
      case Some(x) =>
        response = OkRestResponse(ProviderResponseEntity(x.asProtocolProvider()))
      case None =>
    }

    response
  }

  override def delete(name: String): RestResponse = {
    var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
      createMessage("rest.providers.provider.cannot.delete", name)))
    val services = getRelatedServices(name)

    if (services.isEmpty) {
      val provider = entityDAO.get(name)

      provider match {
        case Some(_) =>
          entityDAO.delete(name)
          response = OkRestResponse(MessageResponseEntity(
            createMessage("rest.providers.provider.deleted", name)))
        case None =>
          response = NotFoundRestResponse(MessageResponseEntity(
            createMessage("rest.providers.provider.notfound", name)))
      }
    }

    response
  }

  def checkConnection(name: String): RestResponse = {
    val provider = entityDAO.get(name)
    var response: RestResponse = NotFoundRestResponse(
      MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))

    provider match {
      case Some(x) =>
        val errors = x.checkConnection()
        if (errors.isEmpty) {
          response = OkRestResponse(ConnectionResponseEntity())
        }
        else {
          response = ConflictRestResponse(TestConnectionResponseEntity(connection = false, errors.mkString(";")))
        }
      case None =>
    }

    response
  }

  def getRelated(name: String): RestResponse = {
    val provider = entityDAO.get(name)
    var response: RestResponse = NotFoundRestResponse(
      MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))

    provider match {
      case Some(x) =>
        response = OkRestResponse(RelatedToProviderResponseEntity(getRelatedServices(name)))
      case None =>
    }

    response
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
