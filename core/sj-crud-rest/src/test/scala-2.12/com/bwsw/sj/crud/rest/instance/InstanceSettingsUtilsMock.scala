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