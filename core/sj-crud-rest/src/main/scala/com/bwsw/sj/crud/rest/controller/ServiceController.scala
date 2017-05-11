package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.crud.rest.model.service.ServiceApi
import com.bwsw.sj.common.si.ServiceSI
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.ServiceLiterals
import com.bwsw.sj.crud.rest.{RelatedToServiceResponseEntity, ServiceResponseEntity, ServicesResponseEntity}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator

import scala.util.{Failure, Success, Try}

class ServiceController extends Controller {
  override protected val serviceSI = new ServiceSI()

  override def create(serializedEntity: String): RestResponse = {
    var response: RestResponse = new RestResponse()

    val triedServiceData = Try(serializer.deserialize[ServiceApi](serializedEntity))
    triedServiceData match {
      case Success(serviceData) =>
        val isCreated = serviceSI.process(serviceData.asService())

        response = isCreated match {
          case Right(_) =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.services.service.created", serviceData.name)))
          case Left(errors) => BadRequestRestResponse(MessageResponseEntity(
            createMessage("rest.services.service.cannot.create", errors.mkString(";"))
          ))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = JsonDeserializationErrorMessageCreator(exception)
        response = BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.services.service.cannot.create", error)))

      case Failure(exception) => throw exception
    }

    response
  }

  override def getAll(): RestResponse = {
    val response = OkRestResponse(ServicesResponseEntity())
    val services = serviceSI.getAll()
    if (services.nonEmpty) {
      response.entity = ServicesResponseEntity(services.map(p => ServiceApi.fromService(p)))
    }

    response
  }

  override def get(name: String): RestResponse = {
    val service = serviceSI.get(name)

    val response = service match {
      case Some(x) =>
        OkRestResponse(ServiceResponseEntity(ServiceApi.fromService(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.services.service.notfound", name)))
    }

    response
  }

  override def delete(name: String): RestResponse = {
    val deleteResponse = serviceSI.delete(name)
    val response: RestResponse = deleteResponse match {
      case Right(isDeleted) =>
        if (isDeleted)
          OkRestResponse(MessageResponseEntity(createMessage("rest.services.service.deleted", name)))
        else
          NotFoundRestResponse(MessageResponseEntity(createMessage("rest.services.service.notfound", name)))
      case Left(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }

    response
  }

  def getRelated(name: String): RestResponse = {
    val relatedStreamsAndInstances = serviceSI.getRelated(name)
    val response = relatedStreamsAndInstances match {
      case Right(servicesAndInstances) =>
        OkRestResponse(RelatedToServiceResponseEntity(servicesAndInstances._1, servicesAndInstances._2))
      case Left(_) =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.services.service.notfound", name)))
    }

    response
  }

  def getTypes(): RestResponse = {
    OkRestResponse(TypesResponseEntity(ServiceLiterals.types))
  }
}
