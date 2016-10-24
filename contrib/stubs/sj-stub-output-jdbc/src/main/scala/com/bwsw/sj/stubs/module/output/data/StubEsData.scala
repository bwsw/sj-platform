package com.bwsw.sj.stubs.module.output.data

import com.bwsw.sj.engine.core.entities.JdbcEnvelope

/**
  * @author Diryavkin Dmitry
  */
object ID {
  var seq = 0
}

class StubJdbcData extends JdbcEnvelope {
  var value: Int = 0
  var id: Int = 0

  def setId() = {
    id = ID.seq
    ID.seq+=1
  }
}
