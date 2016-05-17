package com.bwsw.sj.common.DAL.model

import java.util.Date

/**
  * Stage of running instance
  * Created: 13/05/2016
  *
  * @author Kseniya Tomskikh
  */
class InstanceStage {
  var state: String = null
  var datetime: Date = null
  var duration: Long = 0
}
