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

import java.util.UUID

import com.bwsw.sj.common.si.model.instance.Instance
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class InstanceAdditionalFieldCreatorTestSuit extends FlatSpec with Matchers with MockitoSugar{

  it should "getFrameworkName() method returns instance name concatenated with framework id using '-' character" in {
    //arrange
    val instanceName = "instance-name"
    val frameworkId = UUID.randomUUID().toString
    val instance = mock[Instance]
    when(instance.name).thenReturn(instanceName)
    when(instance.frameworkId).thenReturn(frameworkId)

    //act
    val frameworkName = InstanceAdditionalFieldCreator.getFrameworkName(instance)

    //assert
    frameworkName shouldBe s"$instanceName-$frameworkId"
  }
}
