package com.bwsw.sj.crud.rest

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.{Task, ExecutionPlan}

/**
  * Created: 4/14/16
  *
  * @author Kseniya Tomskikh
  */
object SjTest {

  def main(args: Array[String]) = {
    val tasks = Map("instance_task_0" -> Task(Map("s1" -> List(1, 2), "s2" -> List(3, 4))))
    val executionPlan = ExecutionPlan(tasks)
    val serializer = new JsonSerializer
    val json = serializer.serialize(executionPlan)
    println(json)
  }

}
