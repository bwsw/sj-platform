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
package com.bwsw.sj.crud.rest.instance.stopper

import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.instance.{InstanceDomainRenewer, InstanceStopper}
import org.scalatest.mockito.MockitoSugar
import scaldi.Injector

class InstanceStopperMock(_marathonManager: MarathonApi,
                          _instanceManager: InstanceDomainRenewer,
                          instance: Instance,
                          marathonAddress: String)(implicit override val injector: Injector)
  extends InstanceStopper(instance, marathonAddress, delay = 100) with MockitoSugar {

  override protected val instanceManager: InstanceDomainRenewer = _instanceManager
  override protected val client: HttpClient = mock[HttpClient]
  override protected val marathonManager: MarathonApi = _marathonManager

  override protected def markInstanceAsStopped(): Unit = super.markInstanceAsStopped()

  override protected def stopFramework(): Unit = super.stopFramework()

  override protected def waitForFrameworkToStop(): Unit = super.waitForFrameworkToStop()
}