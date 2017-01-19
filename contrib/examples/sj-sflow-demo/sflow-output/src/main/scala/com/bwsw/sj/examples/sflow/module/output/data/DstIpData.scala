package com.bwsw.sj.examples.sflow.module.output.data

import com.bwsw.sj.engine.core.entities.Envelope

/**
  * Created by diryavkin_dn on 17.01.17.
  */
class DstIpData(dst_ip_field: String, traffic_field: Int) extends Envelope{
  val dst_ip: String = dst_ip_field
  val traffic: Int = traffic_field
  var id: String = ""
}


