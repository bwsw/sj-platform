package com.bwsw.sj.common.DAL.model.module

import java.util

/**
 * Entity for execution plan of module instance
 *
 *
 * @author Kseniya Tomskikh
 */
class ExecutionPlan {
  var tasks: java.util.Map[String, Task] = new util.HashMap()
  
  def this(tasks: java.util.Map[String, Task]) = {
    this()
    this.tasks = tasks
  }
}
