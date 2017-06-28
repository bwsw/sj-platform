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
package com.bwsw.sj.crud.rest.instance

import java.util.Date

import com.bwsw.sj.common.dal.model.instance.{FrameworkStage, InstanceDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.{EngineLiterals, FrameworkLiterals}
import com.bwsw.sj.crud.rest.common.InstanceRepositoryMock
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}
import org.mockito.ArgumentMatchers._

class InstanceDomainRenewerTestSuit extends FlatSpec with Matchers with InstanceDomainRenewerMocks {

  it should "updateInstanceStatus() method works properly: " +
    "change an instance status and save a new instance to repository" in {
    //arrange
    val instanceDomain: InstanceDomain = mock[InstanceDomain]
    val instance: Instance = mock[Instance]
    when(instance.to()).thenReturn(instanceDomain)

    //act
    instanceManager.updateInstanceStatus(instance, statusStub)

    //assert
    verify(instance, times(1)).status = statusStub
    verify(getInstanceRepository).save(instanceDomain)
  }

  it should "updateInstanceRestAddress() method works properly: " +
    "change an instance rest address field and save a new instance to repository (NONE)" in {
    //arrange
    val restAddress = None
    val instanceDomain: InstanceDomain = mock[InstanceDomain]
    val instance: Instance = mock[Instance]
    when(instance.to()).thenReturn(instanceDomain)

    //act
    instanceManager.updateInstanceRestAddress(instance, restAddress)

    //assert
    verify(instance, times(1)).restAddress = restAddress
    verify(getInstanceRepository).save(instanceDomain)
  }

  it should "updateInstanceRestAddress() method works properly: " +
    "change an instance field of rest address and save a new instance to repository (SOME)" in {
    //arrange
    val restAddress = Some("host")
    val instanceDomain: InstanceDomain = mock[InstanceDomain]
    val instance: Instance = mock[Instance]
    when(instance.to()).thenReturn(instanceDomain)

    //act
    instanceManager.updateInstanceRestAddress(instance, restAddress)

    //assert
    verify(instance, times(1)).restAddress = restAddress
    verify(getInstanceRepository).save(instanceDomain)
  }

  it should "updateFrameworkStage() method works properly: " +
    "change a stage 'duration' field if the stage has got the same status which passed as a parameter" +
    "and save a new instance to repository" in {
    //arrange
    val instanceDomain: InstanceDomain = mock[InstanceDomain]
    val frameworkStage = mock[FrameworkStage]
    when(frameworkStage.state).thenReturn(statusStub)
    when(frameworkStage.datetime).thenReturn(new Date())
    val instance: Instance = mock[Instance]
    when(instance.stage).thenReturn(frameworkStage)
    when(instance.to()).thenReturn(instanceDomain)

    //act
    instanceManager.updateFrameworkStage(instance, statusStub)

    //assert
    verify(instance.stage, times(1)).duration = anyLong()
    verify(getInstanceRepository).save(instanceDomain)
  }

  it should "updateFrameworkStage() method works properly: " +
    "change all stage fields if the stage has got a different status which passed as a parameter" +
    "and save a new instance to repository" in {
    //arrange
    val differentStatus = "different_status"
    val instanceDomain: InstanceDomain = mock[InstanceDomain]
    val frameworkStage = mock[FrameworkStage]
    when(frameworkStage.state).thenReturn(statusStub)
    val instance: Instance = mock[Instance]
    when(instance.stage).thenReturn(frameworkStage)
    when(instance.to()).thenReturn(instanceDomain)

    //act
    instanceManager.updateFrameworkStage(instance, differentStatus)

    //assert
    differentStatus should not equal statusStub
    verify(instance.stage, times(1)).state = differentStatus
    verify(instance.stage, times(1)).datetime = any()
    verify(instance.stage, times(1)).duration = FrameworkLiterals.initialStageDuration
    verify(getInstanceRepository).save(instanceDomain)
  }

  it should "delete() method works properly" in {
    //arrange
    val instanceName = getInstanceRepository.getAll.head.name

    //act
    instanceManager.deleteInstance(instanceName)

    //assert
    val deletedInstance = getInstanceRepository.get(instanceName)
    deletedInstance shouldBe None
  }
}

trait InstanceDomainRenewerMocks extends MockitoSugar {
  private val instanceRepositoryMock = new InstanceRepositoryMock()

  private val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepositoryMock.repository)

  private val module = new Module {
    bind[ConnectionRepository] to connectionRepository
  }
  private val injector: Injector = module.injector

  val instanceManager = new InstanceDomainRenewer()(injector)

  def getInstanceRepository: GenericMongoRepository[InstanceDomain] = instanceRepositoryMock.repository

  val statusStub: String = EngineLiterals.starting
}
