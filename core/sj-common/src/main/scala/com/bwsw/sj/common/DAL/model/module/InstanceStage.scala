package com.bwsw.sj.common.DAL.model.module

import java.util.Date

/**
 * Stage of running instance
 *
 *
 * @author Kseniya Tomskikh
 */
class InstanceStage() {
  var state: String = null
  var datetime: Date = null
  var duration: Long = 0

  def this(state: String, datetime: Date, duration: Long = 0) = {
    this()
    this.state = state
    this.datetime = datetime
    this.duration = duration
  }
}
