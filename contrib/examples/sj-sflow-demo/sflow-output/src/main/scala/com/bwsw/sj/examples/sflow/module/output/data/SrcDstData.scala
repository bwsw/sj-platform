package com.bwsw.sj.examples.sflow.module.output.data

import com.bwsw.sj.engine.core.entities.Envelope

/**
  * Created by diryavkin_dn on 17.01.17.
  */
class SrcDstData(src_as_field: Int, dst_as_field: Int, traffic_field: Int) extends Envelope{
  var src_as: Int = src_as_field
  var dst_as: Int = dst_as_field
  var traffic: Int = traffic_field
}



