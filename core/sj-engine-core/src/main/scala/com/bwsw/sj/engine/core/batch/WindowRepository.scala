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
package com.bwsw.sj.engine.core.batch

import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.engine.core.entities.Window
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/**
  * Provides methods to keep windows that consist of batches
  * Use it in [[BatchStreamingExecutor.onWindow()]] to retrieve
  *
  * @param instance set of settings of a batch streaming module
  */
class WindowRepository(instance: BatchInstance) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val windowPerStream: mutable.Map[String, Window] = createStorageOfWindows()
  val window: Int = instance.window
  val slidingInterval: Int = instance.slidingInterval

  private def createStorageOfWindows(): mutable.Map[String, Window] = {
    logger.debug("Create a storage to keep windows.")
    mutable.Map(instance.getInputsWithoutStreamMode.map(x => (x, new Window(x))): _*)
  }

  def get(stream: String): Window = {
    logger.debug(s"Get a window for stream: '$stream'.")
    windowPerStream(stream)
  }

  private[engine] def put(stream: String, window: Window): Unit = {
    logger.debug(s"Put a window for stream: '$stream'.")
    windowPerStream(stream) = window
  }

  def getAll(): Map[String, Window] = {
    logger.debug(s"Get all windows.")
    Map(windowPerStream.toList: _*)
  }
}
