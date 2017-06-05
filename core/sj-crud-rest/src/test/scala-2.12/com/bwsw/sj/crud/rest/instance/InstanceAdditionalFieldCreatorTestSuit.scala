package com.bwsw.sj.crud.rest.instance

import java.util.UUID

import com.bwsw.common.marathon.MarathonTask
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals
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

  it should s"getRestAddress() method returns a rest address created from marathon task with ${EngineLiterals.httpPrefix}" in {
    //arrange
    val restHost = "127.0.0.1"
    val restPort = 8080
    val leaderTask = MarathonTask("id", restHost, List(restPort))

    //act
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(leaderTask))

    //assert
    restAddress.get shouldBe s"${EngineLiterals.httpPrefix}$restHost:$restPort"
  }

  it should s"getRestAddress() method returns None if no marathon task is passed" in {
    //act
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(None)

    //assert
    restAddress shouldBe None
  }
}
