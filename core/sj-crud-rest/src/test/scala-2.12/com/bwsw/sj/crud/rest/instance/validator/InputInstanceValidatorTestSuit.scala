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
import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.common.SpecificationWithRandomFieldsMock
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class InputInstanceValidatorTestSuit extends FlatSpec with Matchers with InstanceValidatorMocks {
  private val instanceValidator = new InputInstanceValidator()(injector)

  it should "validate() method returns non empty set of errors if instance and specification is blank" in {
    //arrange
    val instance = new InputInstanceWithDefaultFieldsMock().instance
    val specification = new SpecificationWithRandomFieldsMock().specification

    //act
    val errors = instanceValidator.validate(instance, specification)

    //assert
    errors should not be empty
  }

  it should "validate() method returns empty set of errors if instance is filled with appropriate specification" in {
    //arrange
    val zkServiceName = "zk-service"
    val zkService = mock[ZKServiceDomain]
    when(zkService.name).thenReturn(zkServiceName)
    getServiceStorage.save(zkService)

    val instance = new InputInstanceWithDefaultFieldsMock().instance
    when(instance.coordinationService).thenReturn(zkServiceName)

    val specification = new SpecificationWithRandomFieldsMock().specification

    //act
    val errors = instanceValidator.validate(instance, specification)

    //assert
    errors shouldBe empty
  }
}

class InputInstanceWithDefaultFieldsMock() extends MockitoSugar {
  val instance = mock[InputInstance]
  when(instance.name).thenReturn("correct-name")
  when(instance.outputs).thenReturn(Array[String]())
  when(instance.perTaskCores).thenReturn(1)
  when(instance.perTaskRam).thenReturn(1024)
  when(instance.performanceReportingInterval).thenReturn(60000)
  when(instance.parallelism).thenReturn(1, Nil: _*)
  when(instance.checkpointMode).thenReturn(EngineLiterals.everyNthMode)
  when(instance.checkpointInterval).thenReturn(1)
  when(instance.queueMaxSize).thenReturn(271)
  when(instance.defaultEvictionPolicy).thenReturn(EngineLiterals.noneDefaultEvictionPolicy)
  when(instance.evictionPolicy).thenReturn(EngineLiterals.fixTimeEvictionPolicy)
}