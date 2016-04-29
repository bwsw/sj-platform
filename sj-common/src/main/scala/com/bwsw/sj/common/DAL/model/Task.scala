package com.bwsw.sj.common.DAL.model

import scala.collection._

/**
 * Entity for task of execution plan
 * Created: 4/14/16
 *
 * @author Kseniya Tomskikh
 */
case class Task(inputs: mutable.Map[String, List[Int]])
