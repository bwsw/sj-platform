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
package com.bwsw.sj.mesos.framework.task

import java.util.{Calendar, Date}

import com.bwsw.sj.common.rest.{Directory, FrameworkTask}
import com.bwsw.sj.mesos.framework.config.FrameworkConfigNames
import com.typesafe.config.ConfigFactory
import org.apache.mesos.Protos.Resource

import scala.util.Try
import scala.collection.mutable

class Task(taskId: String) {
  private val config = ConfigFactory.load()

  val id: String = taskId
  var state: String = "TASK_STAGING"
  var stateChanged: Long = Calendar.getInstance().getTime.getTime
  var reason: String = ""
  var node: String = ""
  var lastNode: String = ""
  //  var description: InstanceTask = _
  var maxDirectories = Try(config.getInt(FrameworkConfigNames.maxSandboxView)).getOrElse(7)
  var directories: mutable.ListBuffer[Directory] = mutable.ListBuffer()
  var host: Option[String] = None
  var ports: Resource = _


  def update(state: String = state,
             stateChanged: Long = stateChanged,
             reason: String = reason,
             node: String = node,
             lastNode: String = lastNode,
             directory: String = "",
             host: String = this.host.orNull,
             ports: Resource = ports): Unit = {
    this.state = state
    this.stateChanged = stateChanged
    this.reason = reason
    this.node = node
    this.lastNode = lastNode
    this.host = Option(host)
    this.ports = ports
    if (!directories.exists(_.path == directory) && directory.nonEmpty)
      directories.append(
        Directory(new Date(stateChanged).toString, directory))
    if (directories.toList.length > maxDirectories) directories = directories.dropRight(1)
  }

  def toFrameworkTask: FrameworkTask = {
    FrameworkTask(id, state, new Date(stateChanged).toString, reason, node, lastNode, directories)
  }
}
