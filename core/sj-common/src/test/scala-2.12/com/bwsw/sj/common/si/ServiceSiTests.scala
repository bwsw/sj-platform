package com.bwsw.sj.common.si

import java.util.UUID

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.service.{Service, ServiceConversion}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mockito.MockitoSugar
import scaldi.{Injector, Module}

import scala.collection.mutable

class ServiceSiTests extends FlatSpec with Matchers {

}

trait ServiceMocks extends MockitoSugar {
  val nonExistsServiceName = "non-exist-service"

  val initServiceStorageSize = 10
  val serviceStorage: mutable.Buffer[ServiceDomain] = Range(0, initServiceStorageSize).map { _ =>
    val serviceDomain = mock[ServiceDomain]
    when(serviceDomain.name).thenReturn(UUID.randomUUID().toString)
    serviceDomain
  }.toBuffer
  val initServiceStorage: Set[ServiceDomain] = serviceStorage.toSet

  val services = serviceStorage.map { serviceDomain =>
    val service = mock[Service]
    val serviceName = serviceDomain.name
    when(service.name).thenReturn(serviceName)
    service
  }

  val serviceRepository = mock[GenericMongoRepository[ServiceDomain]]
  when(serviceRepository.getAll).thenReturn({
    serviceStorage
  })
  when(serviceRepository.save(any[ServiceDomain]()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      serviceStorage += invocationOnMock.getArgument[ServiceDomain](0)
    })
  when(serviceRepository.delete(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val serviceName = invocationOnMock.getArgument[String](0)
      serviceStorage -= serviceStorage.find(_.name == serviceName).get
    })
  when(serviceRepository.get(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val serviceName = invocationOnMock.getArgument[String](0)
      serviceStorage.find(_.name == serviceName)
    })

  val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getServiceRepository).thenReturn(serviceRepository)

  val serviceConversion = mock[ServiceConversion]
  when(serviceConversion.from(any[ServiceDomain])(any[Injector]))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val serviceDomain = invocationOnMock.getArgument[ServiceDomain](0)
      services.find(_.name == serviceDomain.name).get
    })

  val module = new Module {
    bind[ConnectionRepository] to connectionRepository
    bind[ServiceConversion] to serviceConversion
  }
  val injector = module.injector
  val serviceSI = new ServiceSI()(injector)
}
