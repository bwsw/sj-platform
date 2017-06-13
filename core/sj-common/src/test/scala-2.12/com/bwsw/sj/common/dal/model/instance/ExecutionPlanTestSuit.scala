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

import com.bwsw.sj.common.rest.model.module.TaskStream
import com.bwsw.sj.common.utils.EngineLiterals
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import scala.collection.JavaConverters._

class ExecutionPlanTestSuit extends FlatSpec with Matchers with PrivateMethodTester with ExecutionPlanMocks {
  it should "fillTasks() method creates tasks which will be launched and fill an execution plan with them" in {
    //arrange
    val numberOfStreams = 4
    val namePrefix = "task"
    val inputs = new java.util.HashMap[String, Array[Int]]
    (0 until numberOfStreams).foreach(x => {
      inputs.put(createName(streamNamePrefix, x), Array(0, 0))
    })

    val expectedTasks = (0 until numberOfStreams).map(x => createName(namePrefix, x) -> new Task(inputs)).toMap

    val taskStreams = (0 until numberOfStreams).map(x => {
      TaskStream(createName(streamNamePrefix, x), EngineLiterals.fullStreamMode, 1)
    }).toArray
    val executionPlan = createExecutionPlan

    //act
    val updatedExecutionPlan = executionPlan.fillTasks(taskStreams, (0 until numberOfStreams).map(createName(namePrefix, _)).toSet)

    //assert
    updatedExecutionPlan.tasks.asScala.forall(x => {
      expectedTasks.contains(x._1) && expectedTasks(x._1).equals(x._2)
    }) shouldBe true
  }

  it should "fillTasks() method creates tasks which will be launched and fill an execution plan with them." +
    "Each task contains the interval of stream partitions that in general provide the number of partitions" in {
    //arrange
    val numberOfStreams = 4
    val numberOfPartitions = numberOfStreams
    val namePrefix = "task"
    val expectedTasks = (0 until numberOfStreams).map(x => {
      val inputs = new java.util.HashMap[String, Array[Int]]
      (0 until numberOfStreams).foreach(y => {
        inputs.put(createName(streamNamePrefix, y), Array(x, x))
      })

      createName(namePrefix, x) -> new Task(inputs)
    }).toMap

    val taskStreams = (0 until numberOfStreams).map(x => {
      TaskStream(createName(streamNamePrefix, x), EngineLiterals.splitStreamMode, numberOfPartitions)
    }).toArray
    val executionPlan = createExecutionPlan

    //act
    val updatedExecutionPlan = executionPlan.fillTasks(taskStreams, (0 until numberOfStreams).map(createName(namePrefix, _)).toSet)

    //assert
    updatedExecutionPlan.tasks.asScala.forall(x => {
      expectedTasks.contains(x._1) && expectedTasks(x._1).equals(x._2)
    }) shouldBe true
  }

  it should "createTask() method returns a task that contains a list of inputs with partition interval which will be processed." +
    "The number of task inputs should be equal the number of task streams that has been passed" in {
    //arrange
    val numberOfStreams = 4
    val namePrefix = "stream"
    val inputs = new java.util.HashMap[String, Array[Int]]
    (0 until numberOfStreams).foreach(x => {
      inputs.put(createName(namePrefix, x), Array(0, 0))
    })

    val taskStreams = (0 until numberOfStreams).map(x => {
      TaskStream(createName(namePrefix, x), EngineLiterals.splitStreamMode, 1)
    }).toArray
    val createTask = PrivateMethod[Task]('createTask)
    val executionPlan = createExecutionPlan

    //act
    val task = executionPlan invokePrivate createTask(taskStreams, 1)

    //assert
    task shouldEqual new Task(inputs)
  }

  it should "createPartitionsInterval() method returns a partition interval " +
    "that contains a number of start and end partition for processing by engine" in {
    //arrange
    val expectedInterval = Array(0, 0)
    val taskStream = TaskStream(streamNamePrefix, EngineLiterals.splitStreamMode, 1)
    val createPartitionInterval = PrivateMethod[Array[Int]]('createPartitionInterval)
    val executionPlan = createExecutionPlan

    //act
    val interval = executionPlan invokePrivate createPartitionInterval(taskStream, 1)

    //assert
    interval shouldEqual expectedInterval
  }

  it should "getEndPartition() method returns end partition " +
    "that is equal to the sum of a current partition of stream and the number of available partitions " +
    s"if a stream has got '${EngineLiterals.fullStreamMode}' mode, regardless of the number of non-processed tasks" in {
    //arrange
    val availablePartitions = 10
    val currentPartition = 4
    val nonProcessedTasks = 100
    val taskStream = TaskStream(streamNamePrefix, EngineLiterals.fullStreamMode, availablePartitions, currentPartition)
    val getEndPartition = PrivateMethod[Int]('getEndPartition)
    val executionPlan = createExecutionPlan

    //act
    val endPartition = executionPlan invokePrivate getEndPartition(taskStream, nonProcessedTasks)

    //assert
    endPartition shouldEqual currentPartition + availablePartitions
  }

  it should "getEndPartition() method returns end partition " +
    "that is equal to the sum of a current partition of stream and the number of available partitions " +
    s"if a stream has got '${EngineLiterals.splitStreamMode}' mode and there is only one non-processed task" in {
    //arrange
    val availablePartitions = 10
    val currentPartition = 4
    val nonProcessedTasks = 1
    val taskStream = TaskStream(streamNamePrefix, EngineLiterals.splitStreamMode, availablePartitions, currentPartition)
    val getEndPartition = PrivateMethod[Int]('getEndPartition)
    val executionPlan = createExecutionPlan

    //act
    val endPartition = executionPlan invokePrivate getEndPartition(taskStream, nonProcessedTasks)

    //assert
    endPartition shouldEqual currentPartition + availablePartitions
  }

  it should "getEndPartition() method returns end partition " +
    "that is equal to the sum of a current partition of stream and " +
    "the integer part of dividing the number of available partitions by the number of non-processed tasks " +
    s"if a stream has got '${EngineLiterals.splitStreamMode}' mode and there are more than one non-processed tasks" in {
    //arrange
    val availablePartitions = 10
    val currentPartition = 4
    val nonProcessedTasks = 3
    val taskStream = TaskStream(streamNamePrefix, EngineLiterals.splitStreamMode, availablePartitions, currentPartition)
    val getEndPartition = PrivateMethod[Int]('getEndPartition)
    val executionPlan = createExecutionPlan

    //act
    val endPartition = executionPlan invokePrivate getEndPartition(taskStream, nonProcessedTasks)

    //assert
    endPartition shouldEqual currentPartition + availablePartitions / nonProcessedTasks
  }
}

trait ExecutionPlanMocks extends MockitoSugar {
  val streamNamePrefix = "stream"

  def createName(prefix: String, number: Int): String = {
    prefix + number
  }

  def createExecutionPlan =
    new ExecutionPlan()
}

