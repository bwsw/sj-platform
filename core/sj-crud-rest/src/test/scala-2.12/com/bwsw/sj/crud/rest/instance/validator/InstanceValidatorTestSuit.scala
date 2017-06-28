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
package com.bwsw.sj.crud.rest.instance.validator

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.common._
import org.mockito.Mockito.when
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

class InstanceValidatorTestSuit extends FlatSpec with Matchers with InstanceValidatorMocks {
  private val instanceValidator = new InstanceValidator()(injector) {
    override type T = Instance
  }

  it should "validate() method returns non empty set of errors if instance is blank" in {
    //arrange
    val instance = mock[Instance]
    val specification = new SpecificationWithRandomFieldsMock().specification

    //act
    val errors = instanceValidator.validate(instance, specification)

    //assert
    errors should not be empty
  }

  it should "validate() method returns empty set of errors if instance is filled" in {
    //arrange
    val zkServiceName = "zk_service"
    val zkService = mock[ZKServiceDomain]
    when(zkService.name).thenReturn(zkServiceName)
    getServiceStorage.save(zkService)

    val instance = mock[Instance]
    when(instance.name).thenReturn("name")
    when(instance.coordinationService).thenReturn(zkServiceName)
    when(instance.perTaskCores).thenReturn(1)
    when(instance.perTaskRam).thenReturn(1024)
    when(instance.performanceReportingInterval).thenReturn(60000)

    //act
    val errors = instanceValidator.validate(instance, null)

    //assert
    errors shouldBe empty
  }

  it should s"getStreamServices() method returns empty list if no streams are passed" in {
    //arrange
    val streams: ArrayBuffer[StreamDomain] = ArrayBuffer()

    //act
    val streamServices = instanceValidator.getStreamServices(streams)

    //assert
    streamServices shouldBe empty
  }

  it should s"getStreamServices() method returns the set of services names" in {
    //arrange
    val services: ArrayBuffer[String] = ArrayBuffer()
    val streams = getStreamStorage.getAll
    streams.foreach(services += _.service.name)

    //act
    val streamServices = instanceValidator.getStreamServices(streams)

    //assert
    services should contain theSameElementsAs streamServices
  }
}



