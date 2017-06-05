package com.bwsw.sj.crud.rest.instance.validator

import com.bwsw.sj.common.dal.model.service.{TStreamServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.si.model.instance.OutputInstance
import com.bwsw.sj.common.utils.{EngineLiterals, ServiceLiterals, StreamLiterals}
import com.bwsw.sj.crud.rest.common.SpecificationWithRandomFieldsMock
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class OutputInstanceValidatorTestSuit extends FlatSpec with Matchers with InstanceValidatorMocks {
  private val instanceValidator = new OutputInstanceValidator()(injector)

  it should "validate() method returns non empty set of errors if instance and specification is blank" in {
    //arrange
    val instance = new OutputInstanceWithDefaultFieldsMock().instance
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

    val tstrServiceName = "tstr-service"
    val tstrService = mock[TStreamServiceDomain]
    when(tstrService.name).thenReturn(tstrServiceName)
    when(tstrService.serviceType).thenReturn(ServiceLiterals.tstreamsType)
    getServiceStorage.save(tstrService)
    val streamName = "tstr-stream"
    val stream = mock[TStreamStreamDomain]
    when(stream.name).thenReturn(streamName)
    when(stream.partitions).thenReturn(1)
    when(stream.streamType).thenReturn(StreamLiterals.tstreamType)
    when(stream.service).thenReturn(tstrService)
    getStreamStorage.save(stream)

    val instance = new OutputInstanceWithDefaultFieldsMock().instance
    when(instance.coordinationService).thenReturn(zkServiceName)
    when(instance.output).thenReturn(streamName)
    when(instance.input).thenReturn(streamName)

    val specification = new SpecificationWithRandomFieldsMock().specification

    //act
    val errors = instanceValidator.validate(instance, specification)

    //assert
    errors shouldBe empty
  }
}

class OutputInstanceWithDefaultFieldsMock() extends MockitoSugar {
  val instance = mock[OutputInstance]
  when(instance.name).thenReturn("correct-name")
  when(instance.perTaskCores).thenReturn(1)
  when(instance.perTaskRam).thenReturn(1024)
  when(instance.performanceReportingInterval).thenReturn(60000)
  when(instance.parallelism).thenReturn(1, Nil: _*)
  when(instance.checkpointMode).thenReturn(EngineLiterals.everyNthMode)
  when(instance.checkpointInterval).thenReturn(1)
  when(instance.startFrom).thenReturn(EngineLiterals.newestStartMode)
}
