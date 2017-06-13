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
package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.sj.common.rest.model.module.TaskStream
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonIgnore

/**
  * Entity for execution plan of [[InstanceDomain]] and auxilary methods to fill it
  * [[ExecutionPlan]] doesn't exist in [[InputInstanceDomain]]. It contains [[InputInstanceDomain.tasks]] instead
  *
  * @author Kseniya Tomskikh
  */
class ExecutionPlan(var tasks: java.util.Map[String, Task] = new util.HashMap()) {

  @JsonIgnore
  def fillTasks(taskStreams: Array[TaskStream], taskNames: Set[String]): ExecutionPlan = {
    var nonProcessedTasks = taskNames.size

    taskNames.foreach(taskName => {
      val task = createTask(taskStreams, nonProcessedTasks)
      this.tasks.put(taskName, task)
      nonProcessedTasks -= 1
    })

    this
  }

  private def createTask(taskStreams: Array[TaskStream], nonProcessedTasks: Int): Task = {
    val task = new Task()
    taskStreams.foreach(taskStream => {
      task.addInput(taskStream.name, createPartitionInterval(taskStream, nonProcessedTasks))
    })

    task
  }

  private def createPartitionInterval(taskStream: TaskStream, nonProcessedTasks: Int): Array[Int] = {
    val startPartition = taskStream.currentPartition
    val endPartition = getEndPartition(taskStream, nonProcessedTasks)
    val interval = Array(startPartition, endPartition - 1)

    interval
  }

  private def getEndPartition(taskStream: TaskStream, nonProcessedTasks: Int): Int = {
    val availablePartitionsCount = taskStream.availablePartitionsCount
    val startPartition = taskStream.currentPartition
    var endPartition = startPartition + availablePartitionsCount
    taskStream.mode match {
      case EngineLiterals.splitStreamMode =>
        val cntTaskStreamPartitions = availablePartitionsCount / nonProcessedTasks
        taskStream.availablePartitionsCount -= cntTaskStreamPartitions
        taskStream.currentPartition += cntTaskStreamPartitions

        endPartition = startPartition + cntTaskStreamPartitions

      case EngineLiterals.fullStreamMode =>
    }

    endPartition
  }
}
