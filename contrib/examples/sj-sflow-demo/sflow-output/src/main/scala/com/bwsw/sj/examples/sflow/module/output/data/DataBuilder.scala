package com.bwsw.sj.examples.sflow.module.output.data

import com.bwsw.sj.engine.core.entities.Envelope

/**
  * Created by diryavkin_dn on 17.01.17.
  */

abstract class DataBuilder {
  def build(i: Int):Envelope = ???
}
