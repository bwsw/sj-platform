package com.bwsw.sj.examples.sflow.module.process.mapreduce.reducerFactories

import com.hazelcast.mapreduce.ReducerFactory
import com.bwsw.sj.examples.sflow.module.process.mapreduce.reducers.CommonReducer


class AsReducerFactory extends ReducerFactory[Int, Int, Int] {
  override def newReducer(key: Int) = {
    new CommonReducer()
  }
}
