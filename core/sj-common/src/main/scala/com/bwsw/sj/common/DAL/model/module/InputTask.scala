package com.bwsw.sj.common.DAL.model.module

/**
  * Entity for task of tasks of input instance
  *
  *
  * @author Kseniya Tomskikh
  */
class InputTask(var host: String = "", var port: Int = 0) {
  def clear(): Unit = {
    this.host = ""
    this.port = 0
  }
}
