package com.bwsw.sj.examples.sflow.module.process.mapreduce.reducers

import com.hazelcast.mapreduce.Reducer


class IpReducer extends Reducer[Int, String] {
  var sum: Int = 0

  override def reduce(value: Int) = {
    sum += value
  }

  override def finalizeReduce(): String = {
    sum.toString
  }
}
