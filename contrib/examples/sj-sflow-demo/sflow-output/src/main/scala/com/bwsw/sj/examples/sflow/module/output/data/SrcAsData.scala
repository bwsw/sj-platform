package com.bwsw.sj.examples.sflow.module.output.data

import com.bwsw.sj.engine.core.entities.OutputEnvelope

/**
  * Created by diryavkin_dn on 16.01.17.
  */

class SrcAsData(src_as_field: Int, traffic_field: Int) extends OutputEnvelope {
  var src_as: Int = src_as_field
  var traffic: Int = traffic_field

  def getMapFields: Map[String, Any] = {
    Map("value" -> src_as, "testId" -> traffic)
  }

}

