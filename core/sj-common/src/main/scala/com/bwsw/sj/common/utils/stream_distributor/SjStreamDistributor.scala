package com.bwsw.sj.common.utils.stream_distributor

import com.bwsw.sj.common.utils.AvroUtils
import org.apache.avro.generic.GenericData.Record

/**
  * Distributes envelopes to partitions.
  * Has following distribution policies:
  * 1. Round robin.
  * 2. By-hash.
  *
  * @param partitionCount count of partition
  * @param policy         distribution policy
  * @param fieldNames     field names for by-hash policy
  * @author Pavel Tomskikh
  */
class SjStreamDistributor(
    partitionCount: Int,
    policy: SjStreamDistributionPolicy = RoundRobin,
    fieldNames: Seq[String] = Seq.empty) {

  require(partitionCount > 0, "partitionCount must be positive")
  require(policy != ByHash || fieldNames.nonEmpty, "fieldNames must be nonempty for by-hash distribution")

  private var currentPartition = -1

  def getNextPartition(record: Option[Record] = None): Int = policy match {
    case RoundRobin =>
      currentPartition = (currentPartition + 1) % partitionCount
      currentPartition
    case ByHash if record.isDefined => positiveMod(
      AvroUtils.concatFields(fieldNames, record.get).hashCode, partitionCount)
    case ByHash =>
      throw new IllegalArgumentException("record must be defined")
    case _ =>
      throw new IllegalStateException("unknown distribution policy")
  }

  private def positiveMod(dividend: Int, divider: Int) = (dividend % divider + divider) % divider
}

trait SjStreamDistributionPolicy

case object RoundRobin extends SjStreamDistributionPolicy

case object ByHash extends SjStreamDistributionPolicy
