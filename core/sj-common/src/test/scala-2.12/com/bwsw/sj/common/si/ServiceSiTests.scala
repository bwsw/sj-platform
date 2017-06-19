/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.si

import java.util.UUID

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.service.{ServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.service.{Service, ServiceCreator}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.{MessageResourceUtils, MessageResourceUtilsMock}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ServiceSiTests extends FlatSpec with Matchers {

  "ServiceSI" should "create correct service" in new ServiceMocks {
    when(service.validate()).thenReturn(ArrayBuffer[String]())

    serviceSI.create(service) shouldBe Created
    serviceStorage.toSet shouldBe (initServiceStorage + serviceDomain)
  }

  it should "not create incorrect service" in new ServiceMocks {
    val errors = ArrayBuffer("Not valid")
    when(service.validate()).thenReturn(errors)

    serviceSI.create(service) shouldBe NotCreated(errors)
    serviceStorage.toSet shouldBe initServiceStorage
  }

  it should "give all services" in new ServiceMocks {
    serviceSI.getAll().toSet shouldBe services.toSet
  }

  it should "give service when it exists" in new ServiceMocks {
    serviceStorage += serviceDomain
    services += service

    serviceSI.get(serviceName) shouldBe Some(service)
  }

  it should "not give service when it does not exists" in new ServiceMocks {
    serviceSI.get(nonExistsServiceName) shouldBe empty
  }

  it should "give empty arrays when service does not have related streams and instances" in
    new ServiceMocksWithRelated {
      serviceSI.getRelated(serviceWithoutRelatedName) shouldBe
        Right((mutable.Buffer.empty[String], mutable.Buffer.empty[String]))
    }

  it should "give related streams when service has them" in new ServiceMocksWithRelated {
    val expected = Right((onlyStreamsRelatedNames.toSet, mutable.Buffer.empty[String]))
    val related = serviceSI.getRelated(serviceWithRelatedOnlyStreamsName)
      .map { case (streams, instances) => (streams.toSet, instances) }
    related shouldBe expected
  }

  it should "give related instances when service has them" in new ServiceMocksWithRelated {
    val expected = Right((mutable.Buffer.empty[String], onlyInstancesRelatedNames.toSet))
    val related = serviceSI.getRelated(serviceWithRelatedOnlyInstancesName)
      .map { case (streams, instances) => (streams, instances.toSet) }
    related shouldBe expected
  }

  it should "give related streams and instances when service has them" in new ServiceMocksWithRelated {
    val expected = Right((bothStreamsRelatedNames.toSet, bothInstancesRelatedNames.toSet))
    val related = serviceSI.getRelated(serviceWithRelatedBothName)
      .map { case (streams, instances) => (streams.toSet, instances.toSet) }
    related shouldBe expected
  }

  it should "tell that service does not exists in getRelated()" in new ServiceMocksWithRelated {
    serviceSI.getRelated(nonExistsServiceName) shouldBe Left(false)
  }

  it should "delete service when it does not have related streams and instances" in new ServiceMocksWithRelated {
    serviceSI.delete(serviceWithoutRelatedName) shouldBe Deleted
    serviceStorage.toSet shouldBe (initServiceStorage - serviceWithoutRelatedDomain)
  }

  it should "not delete service when it does not exists" in new ServiceMocksWithRelated {
    serviceSI.delete(nonExistsServiceName) shouldBe EntityNotFound
    serviceStorage.toSet shouldBe initServiceStorage
  }

  it should "not delete service when it have related streams" in new ServiceMocksWithRelated {
    val deletionError = s"rest.services.service.cannot.delete.due.to.streams:$serviceWithRelatedOnlyStreamsName"

    serviceSI.delete(serviceWithRelatedOnlyStreamsName) shouldBe DeletionError(deletionError)
    serviceStorage.toSet shouldBe initServiceStorage
  }

  it should "not delete service when it have related instances" in new ServiceMocksWithRelated {
    val deletionError = s"rest.services.service.cannot.delete.due.to.instances:$serviceWithRelatedOnlyInstancesName"

    serviceSI.delete(serviceWithRelatedOnlyInstancesName) shouldBe DeletionError(deletionError)
    serviceStorage.toSet shouldBe initServiceStorage
  }

  it should "not delete service when it have related streams and instances" in new ServiceMocksWithRelated {
    val deletionError = s"rest.services.service.cannot.delete.due.to.streams:$serviceWithRelatedBothName"

    serviceSI.delete(serviceWithRelatedBothName) shouldBe DeletionError(deletionError)
    serviceStorage.toSet shouldBe initServiceStorage
  }

  trait ServiceMocks extends MockitoSugar {
    val nonExistsServiceName = "non-exist-service"

    val serviceName = "service-name"
    val serviceDomain = mock[ServiceDomain]
    when(serviceDomain.name).thenReturn(serviceName)
    val service = mock[Service]
    when(service.name).thenReturn(serviceName)
    when(service.to()).thenReturn(serviceDomain)

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

    val createService = mock[ServiceCreator]
    when(createService.from(any[ServiceDomain])(any[Injector]))
      .thenAnswer((invocationOnMock: InvocationOnMock) => {
        val serviceDomain = invocationOnMock.getArgument[ServiceDomain](0)
        services.find(_.name == serviceDomain.name).get
      })

    val module = new Module {
      bind[ConnectionRepository] to connectionRepository
      bind[MessageResourceUtils] to MessageResourceUtilsMock.messageResourceUtils
      bind[ServiceCreator] to createService
    }
    val injector = module.injector
    val serviceSI = new ServiceSI()(injector)
  }

  trait ServiceMocksWithRelated extends ServiceMocks {
    val serviceWithoutRelatedName = "service-without-related"
    val serviceWithoutRelatedDomain = mock[ServiceDomain]
    when(serviceWithoutRelatedDomain.name).thenReturn(serviceWithoutRelatedName)

    val serviceWithRelatedOnlyStreamsName = "service-with-related-streams"
    val serviceWithRelatedOnlyStreamsDomain = mock[ServiceDomain]
    when(serviceWithRelatedOnlyStreamsDomain.name).thenReturn(serviceWithRelatedOnlyStreamsName)

    val streamsForOneService = 5
    val onlyStreamsRelated = Range(0, streamsForOneService).map { _ =>
      val stream = mock[StreamDomain]
      when(stream.name).thenReturn(UUID.randomUUID().toString)
      when(stream.service).thenReturn(serviceWithRelatedOnlyStreamsDomain)
      stream
    }
    val onlyStreamsRelatedNames = onlyStreamsRelated.map(_.name)

    val serviceWithRelatedOnlyInstancesName = "service-with-related-instances"
    val serviceWithRelatedOnlyInstancesDomain = mock[ZKServiceDomain]
    when(serviceWithRelatedOnlyInstancesDomain.name).thenReturn(serviceWithRelatedOnlyInstancesName)

    val instancesForOneService = 5
    val onlyInstancesRelated = Range(0, instancesForOneService).map { _ =>
      val instance = mock[InstanceDomain]
      when(instance.name).thenReturn(UUID.randomUUID().toString)
      when(instance.coordinationService).thenReturn(serviceWithRelatedOnlyInstancesDomain)
      instance
    }
    val onlyInstancesRelatedNames = onlyInstancesRelated.map(_.name)

    val serviceWithRelatedBothName = "service-with-both-related"
    val serviceWithRelatedBothDomain = mock[ZKServiceDomain]
    when(serviceWithRelatedBothDomain.name).thenReturn(serviceWithRelatedBothName)

    val bothStreamsRelated = Range(0, streamsForOneService).map { _ =>
      val stream = mock[StreamDomain]
      when(stream.name).thenReturn(UUID.randomUUID().toString)
      when(stream.service).thenReturn(serviceWithRelatedBothDomain)
      stream
    }
    val bothStreamsRelatedNames = bothStreamsRelated.map(_.name)

    val bothInstancesRelated = Range(0, instancesForOneService).map { _ =>
      val instance = mock[InstanceDomain]
      when(instance.name).thenReturn(UUID.randomUUID().toString)
      when(instance.coordinationService).thenReturn(serviceWithRelatedBothDomain)
      instance
    }
    val bothInstancesRelatedNames = bothInstancesRelated.map(_.name)

    val allStreams = (onlyStreamsRelated ++ bothStreamsRelated).toBuffer
    val streamRepository = mock[GenericMongoRepository[StreamDomain]]
    when(streamRepository.getAll).thenReturn(allStreams)
    when(connectionRepository.getStreamRepository).thenReturn(streamRepository)

    val allInstances = (onlyInstancesRelated ++ bothInstancesRelated).toBuffer
    val instanceRepository = mock[GenericMongoRepository[InstanceDomain]]
    when(instanceRepository.getAll).thenReturn(allInstances)
    when(connectionRepository.getInstanceRepository).thenReturn(instanceRepository)

    serviceStorage ++= mutable.Buffer(
      serviceWithoutRelatedDomain,
      serviceWithRelatedOnlyStreamsDomain,
      serviceWithRelatedOnlyInstancesDomain,
      serviceWithRelatedBothDomain)
    override val initServiceStorage: Set[ServiceDomain] = serviceStorage.toSet
    override val serviceSI = new ServiceSI()(injector)
  }

}
