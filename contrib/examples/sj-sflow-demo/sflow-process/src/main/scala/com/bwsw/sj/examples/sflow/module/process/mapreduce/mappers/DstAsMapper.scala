package com.bwsw.sj.examples.sflow.module.process.mapreduce.mappers

import com.bwsw.sj.common.utils.SflowRecord
import com.hazelcast.mapreduce.{Context, Mapper}


class DstAsMapper extends Mapper[String, SflowRecord, Int, Int] {
  override def map(key: String, value: SflowRecord, context: Context[Int, Int]) = {
    context.emit(value.dstAs, value.packetSize * value.samplingRate)
  }
}
