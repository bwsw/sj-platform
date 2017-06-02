package com.bwsw.sj.crud.rest.validator.instance

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.common._
import org.mockito.Mockito.when
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

class InstanceValidatorTestSuit extends FlatSpec with Matchers with InstanceValidatorMocks {
  private val defaultStreamMode = EngineLiterals.splitStreamMode

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

  it should s"getStreamMode() method returns $defaultStreamMode if a stream hasn't got a stream mode" in {
    //arrange
    val streamWithoutMode = "stream_name"

    //act
    val streamMode: String = instanceValidator.getStreamMode(streamWithoutMode)

    //assert
    streamMode shouldBe defaultStreamMode
  }

  it should s"getStreamMode() method returns a stream mode of stream that is separated of stream name by '/' " in {
    //arrange
    val streamWithoutMode = "stream_name"
    val actualStreamMode = "stream_mode"
    val streamWithMode = actualStreamMode + "/" + streamWithoutMode

    //act
    val streamMode: String = instanceValidator.getStreamMode(streamWithMode)

    //assert
    streamMode shouldBe actualStreamMode
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






