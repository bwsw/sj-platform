package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ServiceSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.{MessageResourceUtils, ServiceLiterals}
import com.bwsw.sj.crud.rest.model.service.{CreateServiceApi, ServiceApi}
import com.bwsw.sj.crud.rest.{RelatedToServiceResponseEntity, ServiceResponseEntity, ServicesResponseEntity}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ServiceController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  override val serviceInterface = inject[ServiceSI]

  protected val entityDeletedMessage: String = "rest.services.service.deleted"
  protected val entityNotFoundMessage: String = "rest.services.service.notfound"
  private val createServiceApi = inject[CreateServiceApi]

  override def create(serializedEntity: String): RestResponse = {
    val triedServiceApi = Try(serializer.deserialize[ServiceApi](serializedEntity))
    triedServiceApi match {
      case Success(serviceData) =>
        val created = serviceInterface.create(serviceData.to())

        created match {
          case Created =>
            CreatedRestResponse(MessageResponseEntity(
              createMessage("rest.services.service.created", serviceData.name)))
          case NotCreated(errors) =>
            BadRequestRestResponse(MessageResponseEntity(
              createMessageWithErrors("rest.services.service.cannot.create", errors)))
        }

      case Failure(exception: JsonDeserializationException) =>
        val error = jsonDeserializationErrorMessageCreator(exception)
        BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.services.service.cannot.create", error)))

      case Failure(exception) => throw exception
    }
  }

  override def getAll(): RestResponse = {
    val services = serviceInterface.getAll().map(createServiceApi.from)
    OkRestResponse(ServicesResponseEntity(services))
  }

  override def get(name: String): RestResponse = {
    val service = serviceInterface.get(name)

    service match {
      case Some(x) =>
        OkRestResponse(ServiceResponseEntity(createServiceApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def getRelated(name: String): RestResponse = {
    val relatedStreamsAndInstances = serviceInterface.getRelated(name)
    relatedStreamsAndInstances match {
      case Right(servicesAndInstances) =>
        OkRestResponse(RelatedToServiceResponseEntity(servicesAndInstances._1, servicesAndInstances._2))
      case Left(_) =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def getTypes(): RestResponse = {
    OkRestResponse(TypesResponseEntity(ServiceLiterals.types))
  }
}
