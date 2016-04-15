package com.bwsw.sj.common.entities

import scala.collection._
/**
  * Entity for execution plan of module instance
  * Created: 14/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class ExecutionPlan(tasks: mutable.Map[String, Task])
