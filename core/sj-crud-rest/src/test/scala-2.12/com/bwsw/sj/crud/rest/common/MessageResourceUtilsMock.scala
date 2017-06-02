package com.bwsw.sj.crud.rest.common

import com.bwsw.sj.common.utils.MessageResourceUtils
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar

import scala.collection.mutable

object MessageResourceUtilsMock extends MockitoSugar {
  val messageResourceUtils = mock[MessageResourceUtils]
  when(messageResourceUtils.createMessage(anyString(), any[String]()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val message = invocationOnMock.getArgument[String](0)
      val args = invocationOnMock.getArgument[mutable.WrappedArray[String]](1)
      s"$message:${args.mkString(",")}"
    })
}
