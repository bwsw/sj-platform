package com.bwsw.sj.examples.sflow.module.process.mapreduce.mappers

import com.bwsw.sj.common.utils.SflowRecord
import com.hazelcast.mapreduce.{Context, Mapper}


class DstIpMapper extends Mapper[String, SflowRecord, String, Int] {
  override  def map(key: String, value: SflowRecord, context: Context[String, Int]) = {
    context.emit(value.dstIP, value.packetSize * value.samplingRate)
  }
}