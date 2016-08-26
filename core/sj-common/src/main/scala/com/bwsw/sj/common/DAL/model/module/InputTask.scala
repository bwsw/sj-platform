package com.bwsw.sj.common.DAL.model.module

/**
  * Entity for task of tasks of input instance
  *
  *
  * @author Kseniya Tomskikh
  */
class InputTask() {
  var host: String = null
  var port: Int = 0

  def this(host: String, port: Int) = {
    this()
    this.host = host
    this.port = port
  }

}
