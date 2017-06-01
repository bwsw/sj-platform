package com.bwsw.sj.crud.rest.common

import java.util.UUID

import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.module.Specification
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

class SpecificationWithRandomNameMock() extends MockitoSugar {
  val specification: Specification = mock[Specification]
  when(specification.name).thenReturn(UUID.randomUUID().toString)
}

class InstanceWithRandomNameMock() extends MockitoSugar {
  val instance: Instance = mock[Instance]
  when(instance.name).thenReturn(UUID.randomUUID().toString)
}