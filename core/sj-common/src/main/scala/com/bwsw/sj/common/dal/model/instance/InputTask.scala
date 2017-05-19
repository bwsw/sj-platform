package com.bwsw.sj.common.dal.model.instance
import com.bwsw.sj.common.utils.EngineLiterals
/**
  * Entity for task of [[InputInstanceDomain.tasks]]
  * Task info is a tcp address at which [[EngineLiterals.inputStreamingType]] engine can receive messages
  *
  * @param host host on which a task has been launched
  * @param port port on which a task has been launched
  * @author Kseniya Tomskikh
  */

class InputTask(var host: String = "",
                var port: Int = 0) {
  def clear(): Unit = {
    this.host = ""
    this.port = 0
  }
}
