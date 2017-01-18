package com.bwsw.sj.examples.sflow.module.process.mapreduce.mappers

import com.bwsw.sj.common.utils.SflowRecord
import com.hazelcast.mapreduce.{Context, Mapper}

/**
  * Created by diryavkin_dn on 11.01.17.
  */
class SrcIpMapper extends Mapper[String, SflowRecord, String, Int] {
  override  def map(key: String, value: SflowRecord, context: Context[String, Int]) = {
    context.emit(value.srcIP, value.packetSize * value.samplingRate)
  }
}