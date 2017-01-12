package com.bwsw.sj.examples.sflow.module.process.mapreduce.reducers

import com.hazelcast.mapreduce.Reducer


class SrcDstAsReducer extends Reducer[Int, Int Tuple2 Int] {
  var sum: Int = 0

  override def reduce(value: Int) = {
    sum += value
  }

  override def finalizeReduce(): Int Tuple2 Int = {
    Tuple2(sum, sum)
  }
}
