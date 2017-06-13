package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.JsonSerializer
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ServiceSI
import com.bwsw.sj.common.si.model.service.Service
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.common.utils.ServiceLiterals._
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.service.{CreateServiceApi, ServiceApi}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ServiceControllerTests extends FlatSpec with Matchers with MockitoSugar {
  val entityDeletedMessageName = "rest.services.service.deleted"
  val entityNotFoundMessageName = "rest.services.service.notfound"
  val createdMessageName = "rest.services.service.created"
  val cannotCreateMessageName = "rest.services.service.cannot.create"

  val creationError = ArrayBuffer("not created")

  val serializer = mock[JsonSerializer]

  val messageResourceUtils = mock[MessageResourceUtils]
  val createServiceApi = mock[CreateServiceApi]

  val serviceInterface = mock[ServiceSI]
  when(serviceInterface.get(anyString())).thenReturn(None)
  when(serviceInterface.delete(anyString())).thenReturn(EntityNotFound)
  when(serviceInterface.create(any[Service]())).thenReturn(NotCreated(creationError))
  when(serviceInterface.getRelated(anyString())).thenReturn(Left(false))

  val jsonDeserializationErrorMessageCreator = mock[JsonDeserializationErrorMessageCreator]

  val servicesCount = 3
  val allServices = Range(0, servicesCount).map(i => createService(s"name$i"))
  allServices.foreach {
    case ServiceInfo(_, _, service, name) =>
      when(serviceInterface.get(name)).thenReturn(Some(service))
      when(serviceInterface.delete(name)).thenReturn(Deleted)
      when(serviceInterface.getRelated(name))
        .thenReturn(Right(mutable.Buffer.empty[String], mutable.Buffer.empty[String]))
  }
  when(serviceInterface.getAll()).thenReturn(allServices.map(_.service).toBuffer)

  val withRelatedServiceName = "with-related-service"
  val relatedStreams = mutable.Buffer("stream1, stream2, stream3")
  val relatedInstances = mutable.Buffer("instance1, instance2, instance3")
  when(serviceInterface.getRelated(withRelatedServiceName)).thenReturn(Right(relatedStreams, relatedInstances))

  val entityDeletedMessages = allServices.map {
    case ServiceInfo(_, _, _, name) =>
      val message = entityDeletedMessageName + "," + name
      when(messageResourceUtils.createMessage(entityDeletedMessageName, name)).thenReturn(message)

      (name, message)
  }

  val newServiceName = "new-service-name"
  val newService = createService(newServiceName)
  when(serviceInterface.create(newService.service)).thenReturn(Created)

  val createdMessage = createdMessageName + "," + newServiceName
  when(messageResourceUtils.createMessage(createdMessageName, newServiceName)).thenReturn(createdMessage)

  val entityNotFoundMessage = entityNotFoundMessageName + "," + newServiceName
  val entityNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
  when(messageResourceUtils.createMessage(entityNotFoundMessageName, newServiceName))
    .thenReturn(entityNotFoundMessage)

  val notValidServiceName = "not-valid-service-name"
  val notValidService = createService(notValidServiceName)

  val notValidMessage = cannotCreateMessageName + "," + notValidServiceName
  when(messageResourceUtils.createMessageWithErrors(cannotCreateMessageName, creationError)).thenReturn(notValidMessage)

  val notDeletedServiceName = "not-deleted-service-name"
  val notDeletedService = createService(notDeletedServiceName)
  val deletionError = "service-not-deleted"
  when(serviceInterface.delete(notDeletedServiceName)).thenReturn(DeletionError(deletionError))

  val incorrectJson = "{not a json}"
  val incorrectJsonException = new JsonDeserializationException("json is incorrect")
  val incorrectJsonError = "error: json is incorrect"
  when(serializer.deserialize[ServiceApi](incorrectJson)).thenAnswer(_ => throw incorrectJsonException)
  when(jsonDeserializationErrorMessageCreator.apply(incorrectJsonException)).thenReturn(incorrectJsonError)

  val incorrectJsonMessage = cannotCreateMessageName + "," + incorrectJson
  when(messageResourceUtils.createMessage(cannotCreateMessageName, incorrectJsonError)).thenReturn(incorrectJsonMessage)

  val injector = new Module {
    bind[JsonSerializer] to serializer
    bind[MessageResourceUtils] to messageResourceUtils
    bind[JsonDeserializationErrorMessageCreator] to jsonDeserializationErrorMessageCreator
    bind[ServiceSI] to serviceInterface
    bind[CreateServiceApi] to createServiceApi
  }.injector

  val controller = new ServiceController()(injector)


  // create
  "ServiceController" should "tell that valid service created" in {
    val expected = CreatedRestResponse(MessageResponseEntity(createdMessage))
    controller.create(newService.serialized) shouldBe expected
  }

  it should "tell that service is not a valid" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(notValidMessage))
    controller.create(notValidService.serialized) shouldBe expected
  }

  it should "tell that serialized to json service is not a correct" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(incorrectJsonMessage))
    controller.create(incorrectJson) shouldBe expected
  }

  // getAll
  it should "give all services" in {
    val expected = OkRestResponse(ServicesResponseEntity(allServices.map(_.api).toBuffer))
    controller.getAll() shouldBe expected
  }

  // get
  it should "give service if it is exists" in {
    allServices.foreach {
      case ServiceInfo(_, api, _, name) =>
        controller.get(name) shouldBe OkRestResponse(ServiceResponseEntity(api))
    }
  }

  it should "not give service if it does not exists" in {
    controller.get(newServiceName) shouldBe entityNotFoundResponse
  }

  // getRelated
  it should "give streams and instances related to service" in {
    val expected = OkRestResponse(RelatedToServiceResponseEntity(relatedStreams, relatedInstances))
    controller.getRelated(withRelatedServiceName) shouldBe expected
  }

  it should "give empty buffer if service does not have related services" in {
    allServices.foreach {
      case ServiceInfo(_, _, _, name) =>
        controller.getRelated(name) shouldBe OkRestResponse(RelatedToServiceResponseEntity())
    }
  }

  it should "tell that service does not exists (getRelated)" in {
    controller.getRelated(newServiceName) shouldBe entityNotFoundResponse
  }

  // delete
  it should "delete service if it exists" in {
    allServices.foreach {
      case ServiceInfo(_, _, _, name) =>
        val message = entityDeletedMessageName + "," + name
        when(messageResourceUtils.createMessage(entityDeletedMessageName, name)).thenReturn(message)

        controller.delete(name) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete service if it does not exists" in {
    controller.delete(newServiceName) shouldBe entityNotFoundResponse
  }

  it should "report in a case some deletion error" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.delete(notDeletedServiceName) shouldBe expected
  }

  // getTypes
  it should "get all service types" in {
    val expected = OkRestResponse(TypesResponseEntity(types))
    controller.getTypes() shouldBe expected
  }


  def createService(name: String) = {
    val serviceType = zookeeperType
    val serialized = s"""{"name":"$name","type":"$serviceType"}"""

    val service = mock[Service]
    when(service.name).thenReturn(name)
    when(service.serviceType).thenReturn(serviceType)

    val api = mock[ServiceApi]
    when(api.name).thenReturn(name)
    when(api.serviceType).thenReturn(serviceType)
    when(api.to()(any[Injector]())).thenReturn(service)

    when(serializer.deserialize[ServiceApi](serialized)).thenReturn(api)
    when(createServiceApi.from(service)).thenReturn(api)

    ServiceInfo(serialized, api, service, name)
  }

  case class ServiceInfo(serialized: String, api: ServiceApi, service: Service, name: String)

}
