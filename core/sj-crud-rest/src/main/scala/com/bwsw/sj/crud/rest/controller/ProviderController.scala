package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ProviderSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.provider.{CreateProviderApi, ProviderApi}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ProviderController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  override val serviceInterface = inject[ProviderSI]

  override protected val entityDeletedMessage: String = "rest.providers.provider.deleted"
  override protected val entityNotFoundMessage: String = "rest.providers.provider.notfound"

  private val createProviderApi = inject[CreateProviderApi]

  def create(serializedEntity: String): RestResponse = {
    val triedProviderApi = Try(serializer.deserialize[ProviderApi](serializedEntity))
    triedProviderApi match {
      case Success(providerData) =>
        val created = serviceInterface.create(providerData.to())

        created match {
          case Created =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.created", providerData.name)))
          case NotCreated(errors) =>
            BadRequestRestResponse(MessageResponseEntity(
              createMessageWithErrors("rest.providers.provider.cannot.create", errors)))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = jsonDeserializationErrorMessageCreator(exception)
        BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.providers.provider.cannot.create", error)))

      case Failure(exception) => throw exception
    }
  }

  def getAll(): RestResponse = {
    val providers = serviceInterface.getAll().map(createProviderApi.from)
    OkRestResponse(ProvidersResponseEntity(providers))
  }

  def get(name: String): RestResponse = {
    val provider = serviceInterface.get(name)

    provider match {
      case Some(x) =>
        OkRestResponse(ProviderResponseEntity(createProviderApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def checkConnection(name: String): RestResponse = {
    val connectionResponse = serviceInterface.checkConnection(name)

    connectionResponse match {
      case Right(isFound) =>
        if (isFound) {
          OkRestResponse(ConnectionSuccessResponseEntity)
        }
        else {
          NotFoundRestResponse(
            MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
        }
      case Left(message) =>
        ConflictRestResponse(ConnectionFailedResponseEntity(message.mkString(";")))
    }
  }

  def getRelated(name: String): RestResponse = {
    val relatedServices = serviceInterface.getRelated(name)
    relatedServices match {
      case Right(services) =>
        OkRestResponse(RelatedToProviderResponseEntity(services))
      case Left(_) =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def getTypes(): RestResponse = {
    OkRestResponse(TypesResponseEntity(ProviderLiterals.types))
  }
}


