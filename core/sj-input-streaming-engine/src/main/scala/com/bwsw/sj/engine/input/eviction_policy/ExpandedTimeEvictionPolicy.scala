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
import com.bwsw.sj.common.dal.model.instance.InputInstanceDomain

/**
  * Provides methods are responsible for an expanded time eviction policy of input envelope duplicates
  * In this case time, within which a specific key is kept, will increase if a duplicate appears
  *
  * @param instance Input instance contains a settings of an eviction policy
  *                 (message TTL [[InputInstanceDomain.lookupHistory]],
  *                 a default eviction policy [[InputInstanceDomain.defaultEvictionPolicy]],
  *                 a maximum size of message queue [[InputInstanceDomain.queueMaxSize]],
  *                 async and sync backup count [[InputInstanceDomain.asyncBackupCount]] [[InputInstanceDomain.backupCount]])
  */

class ExpandedTimeEvictionPolicy(instance: InputInstance, hazelcast: HazelcastInterface)
  extends InputInstanceEvictionPolicy(instance, hazelcast) {
  /**
    * Checks whether a specific key is duplicate or not and if it is update a value by the key
    *
    * @param key Key that will be checked
    * @return True if the key is not duplicate and false in other case
    */
  def checkForDuplication(key: String): Boolean = {
    logger.debug(s"Check for duplicate a key: $key.")
    if (!uniqueEnvelopes.containsKey(key)) {
      logger.debug(s"The key: $key is not duplicate.")
      uniqueEnvelopes.set(key, stubValue)
      true
    }
    else {
      logger.debug(s"The key: $key is duplicate so update the TTL of key.")
      uniqueEnvelopes.set(key, stubValue)
      false
    }
  }
}
