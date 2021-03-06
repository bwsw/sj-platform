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

/**
  * Entity for task of [[InputInstanceDomain.tasks]]
  * The host and port defines an address to which the data should be sent
  * in order to [[com.bwsw.sj.common.utils.EngineLiterals.inputStreamingType]] module process them.
  *
  * @param host host on which a task has been launched
  * @param port port on which a task has been launched
  * @author Kseniya Tomskikh
  */

class InputTask(var host: String = "",
                var port: Int = 0) {
  def clear(): Unit = {
    this.host = ""
    this.port = 0
  }
}
