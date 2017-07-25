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
package com.bwsw.common.marathon

import com.bwsw.sj.common.utils.FrameworkLiterals
import com.fasterxml.jackson.annotation.JsonProperty

case class MarathonApplication(app: MarathonApplicationInfo)

case class MarathonApplicationInfo(id: String, env: Map[String, String], tasksRunning: Int,
                                   tasks: List[MarathonTask], lastTaskFailure: MarathonTaskFailure)

case class MarathonTask(id: String, host: String, ports: List[Int])

case class MarathonTaskFailure(host: String, message: String, state: String, timestamp: String)

case class MarathonInfo(@JsonProperty("marathon_config") marathonConfig: MarathonConfig)

case class MarathonConfig(master: String)

case class MarathonRequest(id: String,
                           cmd: String,
                           instances: Int,
                           env: Map[String, String],
                           uris: List[String],
                           backoffSeconds: Int = FrameworkLiterals.defaultBackoffSeconds,
                           backoffFactor: Double = FrameworkLiterals.defaultBackoffFactor,
                           maxLaunchDelaySeconds: Int = FrameworkLiterals.defaultMaxLaunchDelaySeconds,
                           cpus: Double = 0.2,
                           mem: Double = 128)

case class MarathonApplicationInstances(instances: Int)