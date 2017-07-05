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
import com.bwsw.sj.common.utils.EngineLiterals.{expandedTimeEvictionPolicy, fixTimeEvictionPolicy}
import com.hazelcast.core.IMap
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for [[InputInstanceEvictionPolicy]]
  *
  * @author Pavel Tomskikh
  */
class InputInstanceEvictionPolicyTests extends FlatSpec with Matchers with MockitoSugar {
  "InputInstanceEvictionPolicy.apply()" should "create FixTimeEvictionPolicy properly" in new HazelcastMock {
    val evictionPolicy = InputInstanceEvictionPolicy(fixTimeEvictionPolicy, hazelcast)

    evictionPolicy shouldBe a[FixTimeEvictionPolicy]
    evictionPolicy.getUniqueEnvelopes shouldBe hazelcastMap
  }

  it should "create ExpandedTimeEvictionPolicy properly" in new HazelcastMock {
    val evictionPolicy = InputInstanceEvictionPolicy(expandedTimeEvictionPolicy, hazelcast)

    evictionPolicy shouldBe an[ExpandedTimeEvictionPolicy]
    evictionPolicy.getUniqueEnvelopes shouldBe hazelcastMap
  }

  it should "throw RuntimeException for unknown value of the evictionPolicy" in new HazelcastMock {
    a[RuntimeException] shouldBe thrownBy {
      InputInstanceEvictionPolicy("unknown-eviction-policy", hazelcast)
    }
  }

  trait HazelcastMock {
    val hazelcastMap = mock[IMap[String, String]]
    val hazelcast = mock[HazelcastInterface]
    when(hazelcast.getMap).thenReturn(hazelcastMap)
  }

}
