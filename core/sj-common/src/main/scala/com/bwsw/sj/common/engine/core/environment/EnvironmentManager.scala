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
package com.bwsw.sj.common.engine.core.environment

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import org.slf4j.{Logger, LoggerFactory}

/**
  * A common class providing for user methods that can be used in a module of specific type
  *
  * @param options     user defined options from instance [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.options]]
  * @param outputs     set of output streams [[com.bwsw.sj.common.dal.model.stream.StreamDomain]] from instance
  *                    [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.outputs]]
  * @param fileStorage file storage
  * @author Kseniya Mikhaleva
  */

class EnvironmentManager(val options: String, val outputs: Array[StreamDomain], val fileStorage: FileStorage) {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  var isCheckpointInitiated: Boolean = false

  def initiateCheckpoint(): Unit = {
    logger.debug("Initiate a checkpoint manually.")
    isCheckpointInitiated = true
  }

  /**
    * Returns a set of names of the output streams according to the set of tags
    */
  def getStreamsByTags(tags: Array[String]): Array[String] = {
    logger.info(s"Get names of the streams that have set of tags: ${tags.mkString(",")}\n")
    outputs.filter(x => tags.forall(x.tags.contains)).map(_.name)
  }
}
