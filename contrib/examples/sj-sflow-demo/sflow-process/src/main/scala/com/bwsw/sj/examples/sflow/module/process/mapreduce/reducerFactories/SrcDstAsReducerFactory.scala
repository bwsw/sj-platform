package com.bwsw.sj.examples.sflow.module.process.mapreduce.reducerFactories

import com.hazelcast.mapreduce.ReducerFactory
import com.bwsw.sj.examples.sflow.module.process.mapreduce.reducers.SrcDstAsReducer


class SrcDstAsReducerFactory extends ReducerFactory[Int Tuple2 Int, Int, Int] {
  override def newReducer(key: Int Tuple2 Int) = {
    new CommonReducer()
  }
}
