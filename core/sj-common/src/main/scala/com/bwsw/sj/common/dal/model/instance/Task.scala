package com.bwsw.sj.common.dal.model.instance

import scala.collection.JavaConverters._

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

  override def equals(obj: scala.Any): Boolean = obj match {
    case anotherTask: Task =>
      anotherTask.inputs.asScala.forall((x: (String, Array[Int])) => {
        if (inputs.containsKey(x._1)) {
          val currentInput = inputs.get(x._1)

          currentInput.sameElements(x._2)
        } else {

          false
        }
      })

    case _ => super.equals(obj)
  }
}