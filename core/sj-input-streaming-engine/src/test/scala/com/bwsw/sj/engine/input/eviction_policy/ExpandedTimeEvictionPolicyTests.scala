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

import com.bwsw.common.hazelcast.{Hazelcast, HazelcastConfig}
import com.bwsw.sj.common.utils.EngineLiterals.lruDefaultEvictionPolicy
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

/**
  * Tests for [[ExpandedTimeEvictionPolicy]]
  *
  * @author Pavel Tomskikh
  */
class ExpandedTimeEvictionPolicyTests
  extends FlatSpec
    with Matchers
    with TableDrivenPropertyChecks
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val ttlSeconds = 3
  val ttlMillis = ttlSeconds * 1000
  val halfTtlMillis = ttlMillis / 2

  val asyncBackupCount = 1
  val backupCount = 1
  val evictionPolicy = lruDefaultEvictionPolicy
  val maxSize = 3000
  val tcpIpMembers = Seq("localhost")
  val hazelcastConfig = HazelcastConfig(ttlSeconds, asyncBackupCount, backupCount, evictionPolicy, maxSize, tcpIpMembers)

  val mapName = "ExpandedTimeEvictionPolicyTestMap"
  val hazelcast = new Hazelcast(mapName, hazelcastConfig)
  val hazelcastMap = hazelcast.getMap

  val key1 = "key1"
  val key2 = "key2"
  val key3 = "key3"
  val key4 = "key4"
  val allKeys = Table("key", key1, key2, key3, key4)

  override def beforeEach(): Unit = hazelcastMap.clear()

  "ExpandedTimeEvictionPolicy" should "add keys in hazelcast properly" in {
    val evictionPolicy = new ExpandedTimeEvictionPolicy(hazelcast)

    forAll(allKeys) { key =>
      evictionPolicy.isDuplicate(key) shouldBe false
    }

    Thread.sleep(1000)

    forAll(allKeys) { key =>
      evictionPolicy.isDuplicate(key) shouldBe true
    }
  }

  it should "not evict not expired elements" in {
    val evictionPolicy = new ExpandedTimeEvictionPolicy(hazelcast)
    forAll(allKeys)(evictionPolicy.isDuplicate)

    Thread.sleep(halfTtlMillis)
    forAll(allKeys) { key =>
      evictionPolicy.isDuplicate(key) shouldBe true
    }
  }

  it should "evict expired elements" in {
    val evictionPolicy = new ExpandedTimeEvictionPolicy(hazelcast)
    forAll(allKeys)(evictionPolicy.isDuplicate)

    Thread.sleep(ttlMillis)
    forAll(allKeys) { key =>
      evictionPolicy.isDuplicate(key) shouldBe false
    }
  }

  it should "update hazelcast entries' ttl" in {
    val evictionPolicy = new ExpandedTimeEvictionPolicy(hazelcast)

    val keysToUpdate = Table(
      ("key", "update"),
      (key1, true),
      (key2, false),
      (key3, true),
      (key4, false))

    forAll(keysToUpdate)((key, _) => evictionPolicy.isDuplicate(key))

    Thread.sleep(halfTtlMillis)
    forAll(keysToUpdate) { (key, update) =>
      if (update)
        evictionPolicy.isDuplicate(key)
    }

    Thread.sleep(halfTtlMillis)
    forAll(keysToUpdate) { (key, update) =>
      evictionPolicy.isDuplicate(key) shouldBe update
    }
  }

  override def afterAll(): Unit = hazelcast.shutdown()
}
