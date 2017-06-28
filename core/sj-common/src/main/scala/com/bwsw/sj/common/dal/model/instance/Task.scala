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

import scala.collection.JavaConverters._

/**
  * Entity for task of [[ExecutionPlan]]
  *
  * @param inputs set of (stream name -> the number of partitions) from which a task will read messages
  *               stream names are unique
  * @author Kseniya Tomskikh
  */
class Task(var inputs: java.util.Map[String, Array[Int]] = new java.util.HashMap()) {

  def addInput(name: String, partitions: Array[Int]): Array[Int] = {
    inputs.put(name, partitions)
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case anotherTask: Task =>
      anotherTask.inputs.asScala.forall((x: (String, Array[Int])) => {
        if (inputs.containsKey(x._1)) {
          val currentInput = inputs.get(x._1)

          currentInput.sameElements(x._2)
        } else {

          false
        }
      })

    case _ => super.equals(obj)
  }
}