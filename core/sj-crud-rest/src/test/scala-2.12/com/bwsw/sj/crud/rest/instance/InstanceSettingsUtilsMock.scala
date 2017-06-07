package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.crud.rest.instance.InstanceSettingsUtilsMock._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

object InstanceSettingsUtilsMock {
  val (backoffSecondsStub, backoffFactorStub, maxLaunchDelaySecondsStub) = (5, 1.5, 1000)

  val frameworkJarNameStub = "framework.jar"
  val crudRestHostStub = "rest-host"
  val crudRestPortStub = 18080
}

class InstanceSettingsUtilsMock extends MockitoSugar {
  val settingsUtils: SettingsUtils = mock[SettingsUtils]

  when(settingsUtils.getFrameworkMaxLaunchDelaySeconds()).thenReturn(maxLaunchDelaySecondsStub)
  when(settingsUtils.getFrameworkBackoffFactor()).thenReturn(backoffFactorStub)
  when(settingsUtils.getFrameworkBackoffSeconds()).thenReturn(backoffSecondsStub)
  when(settingsUtils.getBackoffSettings()).thenReturn((backoffSecondsStub, backoffFactorStub, maxLaunchDelaySecondsStub))

  when(settingsUtils.getFrameworkJarName()).thenReturn(frameworkJarNameStub)

  when(settingsUtils.getCrudRestHost()).thenReturn(crudRestHostStub)
  when(settingsUtils.getCrudRestPort()).thenReturn(crudRestPortStub)
}