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
package com.bwsw.sj.crud.rest.instance.starter

import com.bwsw.common.LeaderLatch
import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.instance.{InstanceDomainRenewer, InstanceStarter}
import org.scalatest.mockito.MockitoSugar
import scaldi.Injector

class InstanceStarterMock(_marathonManager: MarathonApi,
                          _instanceManager: InstanceDomainRenewer,
                          instance: Instance,
                          marathonAddress: String)(implicit override val injector: Injector)
  extends InstanceStarter(instance, marathonAddress, delay = 100) with MockitoSugar {

  override protected val instanceManager: InstanceDomainRenewer = _instanceManager
  override protected val client: HttpClient = mock[HttpClient]
  override protected val marathonManager: MarathonApi = _marathonManager

  override def createLeaderLatch(marathonMaster: String): LeaderLatch = mock[LeaderLatch]

  override def startInstance(): Unit = super.startInstance()

  override def startFramework(marathonMaster: String, zookeeperAddress: String): Unit = super.startFramework(marathonMaster, zookeeperAddress)

  override def launchExistingFramework(): Unit = super.launchExistingFramework()

  override protected def createFramework(marathonMaster: String, zookeeperAddress: String): Unit = super.createFramework(marathonMaster, zookeeperAddress)

  override protected def checkIsFrameworkLaunched(isStartedOrCreate: Boolean): Unit = super.checkIsFrameworkLaunched(isStartedOrCreate)

  override protected def waitForFrameworkToStart(): Unit = super.waitForFrameworkToStart()
}