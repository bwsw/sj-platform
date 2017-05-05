package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.JsonSerializer
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.bll.ProviderService
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.provider.ProviderData
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator

class ProviderController extends Controller {
  private val serializer = new JsonSerializer()
  override val service = new ProviderService()

  def post(serializedEntity: String): RestResponse = {
    var response: RestResponse = new RestResponse()

    try {
      val data = serializer.deserialize[ProviderData](serializedEntity)
      val isCreated = service.process(data.asModelProvider())

      response = isCreated match {
        case Right(_) =>
          CreatedRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.created", data.name)))
        case Left(errors) => BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.providers.provider.cannot.create", errors.mkString(";"))
        ))
      }
    } catch {
      case e: JsonDeserializationException =>
        val error = JsonDeserializationErrorMessageCreator(e)
        response = BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.providers.provider.cannot.create", error)))
    }

    response
  }

  def getAll(): RestResponse = {
    val response = OkRestResponse(ProvidersResponseEntity())
    val providers = service.getAll()
    if (providers.nonEmpty) {
      response.entity = ProvidersResponseEntity(providers.map(p => ProviderData.fromModelProvider(p)))
    }

    response
  }

  def get(name: String): RestResponse = {
    val provider = service.get(name)

    val response = provider match {
      case Some(x) =>
        OkRestResponse(ProviderResponseEntity(ProviderData.fromModelProvider(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))
    }

    response
  }

  def delete(name: String): RestResponse = {
    val deleteResponse = service.delete(name)
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
    val connectionResponse = service.checkConnection(name)

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
    val relatedServices = service.getRelated(name)
    val response = relatedServices match {
      case Right(services) =>
        OkRestResponse(RelatedToProviderResponseEntity(services))
      case Left(_) =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.notfound", name)))
    }

    response
  }
}


