package com.bwsw.sj.crud.rest.common

import java.util.UUID

import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

class SpecificationWithRandomFieldsMock() extends MockitoSugar {
  val cardinality = mock[IOstream]
  when(cardinality.cardinality).thenReturn(Array(0, 10))
  when(cardinality.types).thenReturn(StreamLiterals.types.toArray)

  val specification: Specification = mock[Specification]
  when(specification.name).thenReturn(UUID.randomUUID().toString)
  when(specification.author).thenReturn(UUID.randomUUID().toString)
  when(specification.version).thenReturn(UUID.randomUUID().toString)
  when(specification.description).thenReturn(RestLiterals.defaultDescription)
  when(specification.engineName).thenReturn(UUID.randomUUID().toString)
  when(specification.engineVersion).thenReturn(UUID.randomUUID().toString)
  when(specification.executorClass).thenReturn(UUID.randomUUID().toString)
  when(specification.validatorClass).thenReturn(UUID.randomUUID().toString)
  when(specification.inputs).thenReturn(cardinality)
  when(specification.license).thenReturn(UUID.randomUUID().toString)
  when(specification.moduleType).thenReturn(UUID.randomUUID().toString)
  when(specification.outputs).thenReturn(cardinality)
}