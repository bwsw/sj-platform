package com.bwsw.sj.examples.sflow.module.output.data

import com.bwsw.sj.engine.core.entities.Envelope

/**
  * Created by diryavkin_dn on 17.01.17.
  */
class SrcIpData(src_ip_field: String, traffic_field: Int) extends Envelope {
  var src_ip: String = src_ip_field
  var traffic: Int = traffic_field

}

