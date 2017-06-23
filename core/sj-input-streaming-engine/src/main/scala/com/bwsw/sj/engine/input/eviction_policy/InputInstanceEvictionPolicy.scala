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
package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.common.hazelcast.HazelcastInterface
import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.hazelcast.core.IMap
import org.slf4j.{Logger, LoggerFactory}

/**
  * Provides methods are responsible for an eviction policy of input envelope duplicates
  *
  * @param instance  Input instance contains a settings of an eviction policy
  *                  (message TTL [[InputInstance.lookupHistory]],
  *                  a default eviction policy [[InputInstance.defaultEvictionPolicy]],
  *                  a maximum size of message queue [[InputInstance.queueMaxSize]],
  *                  async and sync backup count [[InputInstance.asyncBackupCount]] [[InputInstance.backupCount]])
  * @param hazelcast wrapper for hazelcast map
  * @author Kseniya Mikhaleva
  */

abstract class InputInstanceEvictionPolicy(instance: InputInstance, hazelcast: HazelcastInterface) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val stubValue: String = "stub"
  protected val uniqueEnvelopes: IMap[String, String] = getUniqueEnvelopes

  /**
    * Checks whether a specific key is duplicate or not
    *
    * @param key Key that will be checked
    * @return True if the key is not duplicate and false in other case
    */
  def checkForDuplication(key: String): Boolean

  /**
    * Returns a keys storage (Hazelcast map) for checking of there are duplicates (input envelopes) or not
    *
    * @return Storage of keys (Hazelcast map)
    */
  def getUniqueEnvelopes: IMap[String, String] = {
    logger.debug(s"Get a hazelcast map for checking of there are duplicates (input envelopes) or not.")
    hazelcast.getMap
  }
}

object InputInstanceEvictionPolicy {
  /**
    * Creates an eviction policy that defines a way of eviction of duplicate envelope
    *
    * @return Eviction policy of duplicate envelopes
    */
  def apply(instance: InputInstance, hazelcast: HazelcastInterface): InputInstanceEvictionPolicy = {
    instance.evictionPolicy match {
      case EngineLiterals.fixTimeEvictionPolicy => new FixTimeEvictionPolicy(instance, hazelcast)
      case EngineLiterals.expandedTimeEvictionPolicy => new ExpandedTimeEvictionPolicy(instance, hazelcast)
      case _ => throw new RuntimeException(s"There is no eviction policy named: ${instance.evictionPolicy}")
    }
  }
}