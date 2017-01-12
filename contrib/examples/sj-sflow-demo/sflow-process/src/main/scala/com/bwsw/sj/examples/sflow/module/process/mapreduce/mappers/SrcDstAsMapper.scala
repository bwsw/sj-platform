package com.bwsw.sj.examples.sflow.module.process.mapreduce.mappers

import com.bwsw.sj.common.utils.SflowRecord
import com.hazelcast.mapreduce.{Context, Mapper}


class SrcDstAsMapper extends Mapper[String, SflowRecord, Int Tuple2 Int, Int] {
  override def map(key: String, value: SflowRecord, context: Context[Int Tuple2 Int, Int]) = {
    context.emit(Tuple2(value.srcAs, value.dstAs), value.packetSize * value.samplingRate)
  }
}
