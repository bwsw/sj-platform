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
package com.bwsw.sj.common.rest.model.module

/**
  * Class contains the information about input stream
  * that is needed to create [[com.bwsw.sj.common.dal.model.instance.ExecutionPlan]] for launching an instance
  *
  * @param name name of stream
  * @param mode mode of stream, one of [[com.bwsw.sj.common.utils.EngineLiterals.streamModes]]
  * @param availablePartitionsCount current number of partitions available for distributing between the execution plan tasks
  * @param currentPartition number of current partition that the next [[com.bwsw.sj.common.dal.model.instance.Task]] will start
  *                         to create an interval from which the data will be consumed
  */
case class TaskStream(name: String, mode: String, var availablePartitionsCount: Int, var currentPartition: Int = 0)
