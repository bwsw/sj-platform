package com.bwsw.sj.common.dal.model.instance

/**
  * Entity for task of [[ExecutionPlan]]
  *
  * @param inputs set of (stream name -> the number of partitions) from which a task will read messages
  *               stream names are unique
  * @author Kseniya Tomskikh
  */
class Task(var inputs: java.util.Map[String, Array[Int]] = new java.util.HashMap()) {

  def addInput(name: String, partitions: Array[Int]): Array[Int] = {
    inputs.put(name, partitions)
  }
}