package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ProviderSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.provider.ProviderApi
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ProviderController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  override val serviceInterface = new ProviderSI()

  override protected val entityDeletedMessage: String = "rest.providers.provider.deleted"
  override protected val entityNotFoundMessage: String = "rest.providers.provider.notfound"

  def create(serializedEntity: String): RestResponse = {
    var response: RestResponse = new RestResponse()

    val triedProviderApi = Try(serializer.deserialize[ProviderApi](serializedEntity))
    triedProviderApi match {
      case Success(providerData) =>
        val created = serviceInterface.create(providerData.to())

        response = created match {
          case Created =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.created", providerData.name)))
          case NotCreated(errors) =>
            BadRequestRestResponse(MessageResponseEntity(
              createMessageWithErrors("rest.providers.provider.cannot.create", errors)))
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
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
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
            MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
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
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }

    response
  }

  def getTypes(): RestResponse = {
    OkRestResponse(TypesResponseEntity(ProviderLiterals.types))
  }
}


