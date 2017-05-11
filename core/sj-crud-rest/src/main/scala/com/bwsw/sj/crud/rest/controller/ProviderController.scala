package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ProviderSI
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.ProviderLiterals
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.provider.ProviderApi
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator

import scala.util.{Failure, Success, Try}

class ProviderController extends Controller {
  override val serviceInterface = new ProviderSI()

  def create(serializedEntity: String): RestResponse = {
    var response: RestResponse = new RestResponse()

    val triedProviderData = Try(serializer.deserialize[ProviderApi](serializedEntity))
    triedProviderData match {
      case Success(providerData) =>
        val created = serviceInterface.create(providerData.to())

        response = created match {
          case Right(_) =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.created", providerData.name)))
          case Left(errors) => BadRequestRestResponse(MessageResponseEntity(
            createMessage("rest.providers.provider.cannot.create", errors.mkString(";"))
          ))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = JsonDeserializationErrorMessageCreator(exception)
        response = BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.providers.provider.cannot.create", error)))

      case Failure(exception) => throw exception
    }

    response
  }

  def getAll(): RestResponse = {
    val response = OkRestResponse(ProvidersResponseEntity())
    val providers = serviceInterface.getAll()
    if (providers.nonEmpty) {
      response.entity = ProvidersResponseEntity(providers.map(p => ProviderApi.from(p)))
    }

    response
  }

  def get(name: String): RestResponse = {
    val provider = serviceInterface.get(name)

    val response = provider match {
      case Some(x) =>
        OkRestResponse(ProviderResponseEntity(ProviderApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))
    }

    response
  }

  def delete(name: String): RestResponse = {
    val deleteResponse = serviceInterface.delete(name)
    val response: RestResponse = deleteResponse match {
      case Right(isDeleted) =>
        if (isDeleted)
          OkRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.deleted", name)))
        else
          NotFoundRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))
      case Left(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }

    response
  }

  def checkConnection(name: String): RestResponse = {
    val connectionResponse = serviceInterface.checkConnection(name)

    val response = connectionResponse match {
      case Right(isFound) =>
        if (isFound) {
          OkRestResponse(ConnectionResponseEntity())
        }
        else {
          NotFoundRestResponse(
            MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))
        }
      case Left(message) =>
        ConflictRestResponse(TestConnectionResponseEntity(connection = false, message.mkString(";")))
    }

    response
  }

  def getRelated(name: String): RestResponse = {
    val relatedServices = serviceInterface.getRelated(name)
    val response = relatedServices match {
      case Right(services) =>
        OkRestResponse(RelatedToProviderResponseEntity(services))
      case Left(_) =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))
    }

    response
  }

  def getTypes(): RestResponse = {
    OkRestResponse(TypesResponseEntity(ProviderLiterals.types))
  }
}


