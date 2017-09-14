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

/**
  * Provides methods are responsible for a fix time eviction policy of input envelope duplicates.
  * In this case a specific key will be kept within fix time
  *
  * @param hazelcast wrapper for hazelcast map
  */
class FixTimeEvictionPolicy(hazelcast: HazelcastInterface) extends InputInstanceEvictionPolicy(hazelcast) {

  /**
    * Checks whether a specific key is duplicate or not
    *
    * @param key a key that will be checked
    * @return true if the key is duplicate and false in other case
    */
  def isDuplicate(key: String): Boolean = {
    logger.debug(s"Check for duplicate a key: $key.")
    val isDuplicate = uniqueEnvelopes.containsKey(key)
    if (!isDuplicate) uniqueEnvelopes.set(key, ())
    logger.debug(s"Duplication check result: $isDuplicate")

    isDuplicate
  }
}
