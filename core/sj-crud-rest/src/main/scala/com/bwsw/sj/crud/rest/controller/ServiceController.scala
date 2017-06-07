package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ServiceSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.{MessageResourceUtils, ServiceLiterals}
import com.bwsw.sj.crud.rest.model.service.ServiceApi
import com.bwsw.sj.crud.rest.{RelatedToServiceResponseEntity, ServiceResponseEntity, ServicesResponseEntity}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ServiceController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  override val serviceInterface = new ServiceSI()

  protected val entityDeletedMessage: String = "rest.services.service.deleted"
  protected val entityNotFoundMessage: String = "rest.services.service.notfound"

  override def create(serializedEntity: String): RestResponse = {
    var response: RestResponse = new RestResponse()

    val triedServiceApi = Try(serializer.deserialize[ServiceApi](serializedEntity))
    triedServiceApi match {
      case Success(serviceData) =>
        val created = serviceInterface.create(serviceData.to())

        response = created match {
          case Created =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.services.service.created", serviceData.name)))
          case NotCreated(errors) => BadRequestRestResponse(MessageResponseEntity(
            createMessageWithErrors("rest.services.service.cannot.create", errors)
          ))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = jsonDeserializationErrorMessageCreator(exception)
        response = BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.services.service.cannot.create", error)))

      case Failure(exception) => throw exception
    }

    response
  }

  override def getAll(): RestResponse = {
    val response = OkRestResponse(ServicesResponseEntity())
    val services = serviceInterface.getAll()
    if (services.nonEmpty) {
      response.entity = ServicesResponseEntity(services.map(p => ServiceApi.from(p)))
    }

    response
  }

  override def get(name: String): RestResponse = {
    val service = serviceInterface.get(name)

    val response = service match {
      case Some(x) =>
        OkRestResponse(ServiceResponseEntity(ServiceApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }

    response
  }

  def getRelated(name: String): RestResponse = {
    val relatedStreamsAndInstances = serviceInterface.getRelated(name)
    val response = relatedStreamsAndInstances match {
      case Right(servicesAndInstances) =>
        OkRestResponse(RelatedToServiceResponseEntity(servicesAndInstances._1, servicesAndInstances._2))
      case Left(_) =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }

    response
  }

  def getTypes(): RestResponse = {
    OkRestResponse(TypesResponseEntity(ServiceLiterals.types))
  }
}
