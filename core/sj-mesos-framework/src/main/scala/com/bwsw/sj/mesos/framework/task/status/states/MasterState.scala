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
package com.bwsw.sj.mesos.framework.task.status.states

class MasterState(
                   var start_time: Double,
                   var hostname: String,
                   var git_tag: String,
                   var flags: Map[String, Any],
                   var elected_time: Double,
                   var unregistered_frameworks: Array[String],
                   var frameworks: Array[Framework],
                   var deactivated_slaves: Double,
                   var git_sha: String,
                   var build_date: String,
                   var orphan_tasks: Array[Map[String, Any]],
                   var leader: String,
                   var completed_frameworks: Array[Framework],
                   var version: String,
                   var id: String,
                   var pid: String,
                   var build_user: String,
                   var build_time: Double,
                   var activated_slaves: Double,
                   var slaves: Array[Slave],
                   var cluster: String,
                   var log_dir: String)

class Slave(
             var hostname: String,
             var registered_time: Double,
             var offered_resources: Map[String, Any],
             var attributes: Map[String, Any],
             var version: String,
             var id: String,
             var pid: String,
             var reserved_resources: Map[String, Any],
             var unreserved_resources: Map[String, Any],
             var resources: Map[String, Any],
             var used_resources: Map[String, Any],
             var reregistered_time: Double,
             var active: Boolean)
