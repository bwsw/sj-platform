package com.bwsw.sj.examples.sflow.module.output.data

import com.bwsw.sj.engine.core.entities.JdbcEnvelope

/**
  * Created by diryavkin_dn on 16.01.17.
  */

class SrcAsData(srcId: Int, valueSum: Int) extends JdbcEnvelope {
  var src: Int = srcId
  var value: Int = valueSum
  var id: String = ""
}