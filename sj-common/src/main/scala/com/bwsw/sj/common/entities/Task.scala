package com.bwsw.sj.common.entities

import scala.collection._

/**
 * Entity for task of execution plan
 * Created: 4/14/16
 *
 * @author Kseniya Tomskikh
 */
case class Task(inputs: mutable.Map[String, List[Int]])
