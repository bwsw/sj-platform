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

import java.util.Calendar

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.FrameworkLiterals
import scaldi.Injectable.inject
import scaldi.Injector


private[instance] class InstanceDomainRenewer(implicit val injector: Injector) {
  private val instanceRepository = inject[ConnectionRepository].getInstanceRepository

  def updateInstanceStatus(instance: Instance, status: String): Unit = {
    instance.status = status
    instanceRepository.save(instance.to())
  }

  def updateInstanceRestAddress(instance: Instance, restAddress: Option[String]): Unit = {
    instance.restAddress = restAddress
    instanceRepository.save(instance.to())
  }

  def updateFrameworkStage(instance: Instance, status: String): Unit = {
    if (instance.stage.state.equals(status)) {
      instance.stage.duration = Calendar.getInstance().getTime.getTime - instance.stage.datetime.getTime
    } else {
      instance.stage.state = status
      instance.stage.datetime = Calendar.getInstance().getTime
      instance.stage.duration = FrameworkLiterals.initialStageDuration
    }

    instanceRepository.save(instance.to())
  }

  def deleteInstance(name: String): Unit = instanceRepository.delete(name)
}
