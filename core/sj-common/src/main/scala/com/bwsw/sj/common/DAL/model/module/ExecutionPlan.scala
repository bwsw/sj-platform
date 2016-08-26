package com.bwsw.sj.common.DAL.model.module

/**
 * Entity for execution plan of module instance
 *
 *
 * @author Kseniya Tomskikh
 */
class ExecutionPlan {
  var tasks: java.util.Map[String, Task] = null
  
  def this(tasks: java.util.Map[String, Task]) = {
    this()
    this.tasks = tasks
  }
}
