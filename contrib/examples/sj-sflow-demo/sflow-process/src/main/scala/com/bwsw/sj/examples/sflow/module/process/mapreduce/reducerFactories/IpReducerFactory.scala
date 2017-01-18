package com.bwsw.sj.examples.sflow.module.process.mapreduce.reducerFactories

import com.bwsw.sj.examples.sflow.module.process.mapreduce.CommonReducer
import com.hazelcast.mapreduce.ReducerFactory


class IpReducerFactory extends ReducerFactory[String, Int, Int] {
  override def newReducer(key: String) = {
    new CommonReducer()
  }
}
