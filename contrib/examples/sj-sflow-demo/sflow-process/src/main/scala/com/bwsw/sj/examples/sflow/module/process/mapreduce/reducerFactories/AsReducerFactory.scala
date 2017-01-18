package com.bwsw.sj.examples.sflow.module.process.mapreduce.reducerFactories

import com.bwsw.sj.examples.sflow.module.process.mapreduce.CommonReducer
import com.hazelcast.mapreduce.ReducerFactory


class AsReducerFactory extends ReducerFactory[Int, Int, Int] {
  override def newReducer(key: Int) = {
    new CommonReducer()
  }
}
