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
package com.bwsw.sj.engine.core.engine

import java.util.concurrent.Callable

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import org.slf4j.LoggerFactory
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Class is responsible for keeping under surveillance an instance status to be [[com.bwsw.sj.common.utils.EngineLiterals.started]].
  * The status can be changed [[com.bwsw.sj.common.utils.EngineLiterals.instanceStatusModes]] and in this case an engine
  * have to be stopped
  *
  * @param instanceName name of instance that contains the running task name
  */
class InstanceStatusObserver(instanceName: String)(implicit injector: Injector) extends Callable[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def call(): Unit = {
    while (true) {
      logger.debug("Check an instance status in case of an instance failure to stop a work of task.")
      inject[ConnectionRepository].getInstanceRepository.get(instanceName) match {
        case Some(instance) =>
          if (instance.status != EngineLiterals.started) {
            logger.error(s"Task cannot continue to work because of '${instance.status}' status of instance")
            throw new InterruptedException(s"Task cannot continue to work because of '${instance.status}' status of instance")
          }

        case None =>
          throw new UnknownError(s"Instance: $instanceName has been removed. It seems that there is a bug.")
      }

      Thread.sleep(1000)
    }
  }
}
