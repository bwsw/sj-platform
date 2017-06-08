package com.bwsw.sj.crud.rest.instance.stopper

import com.bwsw.common.marathon._
import com.bwsw.sj.common.dal.model.instance.InputTask
import com.bwsw.sj.common.si.model.instance.{InputInstance, Instance}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.instance.InstanceDomainRenewer
import org.apache.http.HttpStatus
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

import scala.collection.mutable

class InstanceStopperTestSuit extends FlatSpec with Matchers with PrivateMethodTester with InstanceStopperMocks {
  it should "hasFrameworkStopped() method returns true if count of running tasks is equal 0" in {
    //arrange
    val emptyMarathonApplicationStub = MarathonApplication(MarathonApplicationInfo(null, Map(), tasksRunning = 0, tasks = List(), lastTaskFailure = null))
    val hasFrameworkStopped = PrivateMethod[Boolean]('hasFrameworkStopped)

    //act
    val frameworkStopped = instanceStopper() invokePrivate hasFrameworkStopped(emptyMarathonApplicationStub)

    //assert
    frameworkStopped shouldBe true
  }

  it should "hasFrameworkStopped() method returns false if count of running tasks isn't equal 0" in {
    //arrange
    val emptyMarathonApplicationStub = MarathonApplication(MarathonApplicationInfo(null, Map(), tasksRunning = 5, tasks = List(), lastTaskFailure = null))
    val hasFrameworkStopped = PrivateMethod[Boolean]('hasFrameworkStopped)

    //act
    val frameworkStopped = instanceStopper() invokePrivate hasFrameworkStopped(emptyMarathonApplicationStub)

    //assert
    frameworkStopped shouldBe false
  }

  it should s"isInputInstance() method returns true if an instance has got '${EngineLiterals.inputStreamingType}' module type" in {
    //arrange
    val isInputInstanceMethod = PrivateMethod[Boolean]('isInputInstance)
    val inputInstance = mock[Instance]
    when(inputInstance.moduleType).thenReturn(EngineLiterals.inputStreamingType)

    //act
    val isInputInstance = instanceStopper(inputInstance) invokePrivate isInputInstanceMethod()

    //assert
    isInputInstance shouldBe true
  }

  it should s"isInputInstance() method returns false if an instance hasn't got '${EngineLiterals.inputStreamingType}' module type" in {
    //arrange
    val isInputInstanceMethod = PrivateMethod[Boolean]('isInputInstance)

    //act
    val isInputInstance = instanceStopper() invokePrivate isInputInstanceMethod()

    //assert
    isInputInstance shouldBe false
  }

  it should s"clearTasks() method clears tasks field if an instance has got '${EngineLiterals.inputStreamingType}' module type" in {
    //arrange
    val clearTasks = PrivateMethod('clearTasks)
    val task = mock[InputTask]
    val tasks = mutable.Map("task" -> task, "task1" -> task, "task2" -> task)
    val inputInstance = mock[InputInstance]
    when(inputInstance.tasks).thenReturn(tasks)

    //act
    instanceStopper(inputInstance) invokePrivate clearTasks()

    //assert
    verify(task, times(tasks.size)).clear()
  }

  it should s"markInstanceAsStopped() method sets instance status to '${EngineLiterals.stopped}'" in {
    //arrange
    val markInstanceAsStopped = PrivateMethod('markInstanceAsStopped)

    val instanceManager = mock[InstanceDomainRenewer]

    //act and assert
    instanceStopperMock(instanceManager = instanceManager) invokePrivate markInstanceAsStopped()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.stopped)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, None)
  }

  it should s"markInstanceAsStopped() method clears instance tasks if it is '${EngineLiterals.inputStreamingType}' " +
    s"and set instance status to '${EngineLiterals.stopped}'" in {
    //arrange
    val markInstanceAsStopped = PrivateMethod('markInstanceAsStopped)
    val task = mock[InputTask]
    val tasks = mutable.Map("task" -> task)
    val inputInstance = mock[InputInstance]
    when(inputInstance.tasks).thenReturn(tasks)
    when(inputInstance.moduleType).thenReturn(EngineLiterals.inputStreamingType)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, instanceMock = inputInstance)

    //act and assert
    instanceStopper invokePrivate markInstanceAsStopped()

    //assert
    verify(task, times(tasks.size)).clear()
    verify(instanceManager, times(1)).updateInstanceStatus(inputInstance, EngineLiterals.stopped)
    verify(instanceManager, times(1)).updateInstanceRestAddress(inputInstance, None)
  }


  it should "stopFramework() method stops the existent marathon app" in {
    //arrange
    val stopFramework = PrivateMethod[String]('stopFramework)


    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(notStartedMarathonApplicationStub)
    when(marathonManager.stopMarathonApplication(frameworkName)).thenReturn(HttpStatus.SC_OK)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act and assert
    instanceStopper invokePrivate stopFramework()

    //assert
    verify(marathonManager, times(1)).stopMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.stopping)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.stopped)
  }

  it should "stopFramework() method fails if marathon has got some problems with the stopping process of framework" in {
    //arrange
    val stopFramework = PrivateMethod[String]('stopFramework)

    val marathonManager = mock[MarathonApi]
    when(marathonManager.stopMarathonApplication(frameworkName)).thenReturn(errorStatus)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act and assert
    assertThrows[Exception](instanceStopper invokePrivate stopFramework())

    //assert
    verify(marathonManager, times(1)).stopMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.error)
  }


  it should "waitForFrameworkToStop() method works properly if there are no problems" in {
    //arrange
    val waitForFrameworkToStop = PrivateMethod('waitForFrameworkToStop)

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(notStartedMarathonApplicationStub)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper invokePrivate waitForFrameworkToStop()

    //assert
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.stopped)
  }

  it should "waitForFrameworkToStop() method do multiple tries to wait until all tasks stop if app tasks don't stop the first time" in {
    //arrange
    val numberOfTries = 5
    val startedMarathonApps = Array.fill(numberOfTries - 1)(marathonApplicationStub).toList
    val waitForFrameworkToStop = PrivateMethod('waitForFrameworkToStop)

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any()))
      .thenReturn(marathonApplicationStub, startedMarathonApps.:+(notStartedMarathonApplicationStub): _*)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper invokePrivate waitForFrameworkToStop()

    //assert
    verify(instanceManager, times(numberOfTries)).updateFrameworkStage(instanceMock, EngineLiterals.stopping)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.stopped)
  }

  it should "waitForFrameworkToStop() method works properly if there has been some problems with stopping process of app tasks" in {
    //arrange
    val waitForFrameworkToStop = PrivateMethod('waitForFrameworkToStop)

    val notOkFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, errorStatus)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(notOkFrameworkResponse)


    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    assertThrows[Exception](instanceStopper invokePrivate waitForFrameworkToStop())

    //assert
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.error)
  }

  it should "run() method works properly if framework has been stopped without any exceptions" in {
    //arrange
    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(notStartedMarathonApplicationStub)
    when(marathonManager.stopMarathonApplication(frameworkName)).thenReturn(HttpStatus.SC_OK)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper.run()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.stopping)
    verify(marathonManager, times(1)).stopMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.stopping)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.stopped)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.stopped)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, None)
  }

  it should s"run() method  doesn't stop an instance (set '${EngineLiterals.error}' status) if there are some exceptions during stop process" in {
    //arrange
    val marathonManager = mock[MarathonApi]
    when(marathonManager.stopMarathonApplication(frameworkName)).thenReturn(errorStatus)

    val instanceManager = mock[InstanceDomainRenewer]
    val instanceStopper = instanceStopperMock(instanceManager = instanceManager, marathonManager = marathonManager)

    //act
    instanceStopper.run()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.stopping)
    verify(marathonManager, times(1)).stopMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.error)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.error)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, None)
  }
}





