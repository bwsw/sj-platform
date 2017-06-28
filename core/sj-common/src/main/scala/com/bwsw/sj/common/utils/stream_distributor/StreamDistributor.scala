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
package com.bwsw.sj.common.utils.stream_distributor

import com.bwsw.sj.common.utils.AvroRecordUtils
import org.apache.avro.generic.GenericData.Record

/**
  * Distributes envelopes to partitions.
  * Has the following distribution policies:
  * 1. [[RoundRobin]]
  * 2. [[ByHash]]
  *
  * @param partitionCount count of partition
  * @param policy         distribution policy [[StreamDistributionPolicy]]
  * @param fieldNames     field names for by-hash policy
  * @author Pavel Tomskikh
  */
class StreamDistributor(partitionCount: Int,
                        policy: StreamDistributionPolicy = RoundRobin,
                        fieldNames: Seq[String] = Seq.empty) {

  require(partitionCount > 0, "partitionCount must be positive")
  require(policy != ByHash || fieldNames.nonEmpty, "fieldNames must be nonempty for by-hash distribution")

  private var currentPartition = -1

  def getNextPartition(record: Option[Record] = None): Int = policy match {
    case RoundRobin =>
      currentPartition = (currentPartition + 1) % partitionCount
      currentPartition
    case ByHash if record.isDefined => positiveMod(
      AvroRecordUtils.concatFields(fieldNames, record.get).hashCode, partitionCount)
    case ByHash =>
      throw new IllegalArgumentException("record must be defined")
    case _ =>
      throw new IllegalStateException("unknown distribution policy")
  }

  private def positiveMod(dividend: Int, divider: Int): Int = (dividend % divider + divider) % divider
}