package com.bwsw.sj.engine.core.entities

/**
  * Created by diryavkin_dn on 15.03.17.
  */
abstract class OutputEnvelope {
  def getMapFields: Map[String, Any]
}
