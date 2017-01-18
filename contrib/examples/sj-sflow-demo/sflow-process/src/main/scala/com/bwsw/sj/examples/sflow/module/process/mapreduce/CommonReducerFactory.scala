package com.bwsw.sj.examples.sflow.module.process.mapreduce

import com.hazelcast.mapreduce.ReducerFactory

/**
  * Created by diryavkin_dn on 18.01.17.
  */
class CommonReducerFactory[KeyIn] extends ReducerFactory[KeyIn, Int, Int] {
  override def newReducer(key: KeyIn) = {
    new CommonReducer()
  }
}