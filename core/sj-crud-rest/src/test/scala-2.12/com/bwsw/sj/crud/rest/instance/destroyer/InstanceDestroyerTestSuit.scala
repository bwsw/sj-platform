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
package com.bwsw.sj.crud.rest.instance.destroyer

import com.bwsw.common.marathon._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.instance.InstanceDomainRenewer
import org.apache.http.HttpStatus
import org.mockito.Mockito._
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

class InstanceDestroyerTestSuit extends FlatSpec with Matchers with PrivateMethodTester with InstanceDestroyerMocks {

  it should "deleteFramework() method removes the existent marathon app" in {
    //arrange
    val deleteFramework = PrivateMethod[String]('deleteFramework)

    val marathonManager = mock[MarathonApi]
    when(marathonManager.destroyMarathonApplication(frameworkName)).thenReturn(okStatus)
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(notFoundFrameworkResponce)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceDestroyerMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act and assert
    instanceStopper invokePrivate deleteFramework()

    //assert
    verify(marathonManager, times(1)).destroyMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.deleting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.deleted)
  }

  it should "deleteFramework() method works properly if a framework app has been deleted earlier" in {
    //arrange
    val deleteFramework = PrivateMethod[String]('deleteFramework)

    val marathonManager = mock[MarathonApi]
    when(marathonManager.destroyMarathonApplication(frameworkName)).thenReturn(HttpStatus.SC_NOT_FOUND)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceDestroyerMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act and assert
    instanceStopper invokePrivate deleteFramework()

    //assert
    verify(marathonManager, times(1)).destroyMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.deleted)
  }

  it should "deleteFramework() method fails if marathon has got some problems with the destroying process of framework" in {
    //arrange
    val deleteFramework = PrivateMethod[String]('deleteFramework)

    val marathonManager = mock[MarathonApi]
    when(marathonManager.destroyMarathonApplication(frameworkName)).thenReturn(errorStatus)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceDestroyerMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act and assert
    assertThrows[Exception](instanceStopper invokePrivate deleteFramework())

    //assert
    verify(marathonManager, times(1)).destroyMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.error)
  }


  it should "waitForFrameworkToDelete() method works properly if there are no problems" in {
    //arrange
    val waitForFrameworkToDelete = PrivateMethod('waitForFrameworkToDelete)

    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(notFoundFrameworkResponce)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceDestroyerMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper invokePrivate waitForFrameworkToDelete()

    //assert
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.deleted)
  }

  it should "waitForFrameworkToDelete() method do multiple tries to wait until marathon destroys the app" in {
    //arrange
    val numberOfTries = 5
    val startedMarathonApps = Array.fill(numberOfTries - 1)(okFrameworkResponse).toList
    val waitForFrameworkToDelete = PrivateMethod('waitForFrameworkToDelete)

    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse, startedMarathonApps.:+(notFoundFrameworkResponce): _*)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceDestroyerMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper invokePrivate waitForFrameworkToDelete()

    //assert
    verify(instanceManager, times(numberOfTries)).updateFrameworkStage(instanceMock, EngineLiterals.deleting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.deleted)
  }

  it should "run() method works properly if framework has been stopped without any exceptions" in {
    //arrange
    val marathonManager = mock[MarathonApi]
    when(marathonManager.destroyMarathonApplication(frameworkName)).thenReturn(okStatus)
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(notFoundFrameworkResponce)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceDestroyerMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper.run()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.deleting)
    verify(marathonManager, times(1)).destroyMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.deleting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.deleted)
    verify(instanceManager, times(1)).deleteInstance(instanceMock.name)
  }

  it should s"run() method  doesn't delete an instance (set '${EngineLiterals.error}' status) if there are some exceptions during destroying process" in {
    //arrange
    val marathonManager = mock[MarathonApi]
    when(marathonManager.destroyMarathonApplication(frameworkName)).thenReturn(errorStatus)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceDestroyerMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper.run()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.deleting)
    verify(marathonManager, times(1)).destroyMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.error)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.error)
  }
}





