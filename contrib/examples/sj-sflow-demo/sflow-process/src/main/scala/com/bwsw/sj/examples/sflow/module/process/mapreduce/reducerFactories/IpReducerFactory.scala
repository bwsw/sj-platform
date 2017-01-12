package com.bwsw.sj.examples.sflow.module.process.mapreduce.reducerFactories

import com.bwsw.sj.examples.sflow.module.process.mapreduce.reducers.IpReducer
import com.hazelcast.mapreduce.ReducerFactory


class IpReducerFactory extends ReducerFactory[String, Int, String] {
  override def newReducer(key: String) = {
    new IpReducer()
  }
}