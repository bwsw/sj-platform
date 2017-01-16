package com.bwsw.sj.examples.sflow.module.output.data

import com.bwsw.sj.engine.core.entities.JdbcEnvelope

/**
  * Created by diryavkin_dn on 16.01.17.
  */

class StubData extends JdbcEnvelope {
  var reducedValue: collection.mutable.Map[Any, Any] = collection.mutable.Map()
  var reduceType: String = ""
  var id: String = ""
}