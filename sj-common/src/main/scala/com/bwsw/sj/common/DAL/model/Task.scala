package com.bwsw.sj.common.DAL.model

/**
 * Entity for task of execution plan
 * Created: 14/04/2016
 *
 * @author Kseniya Tomskikh
 */
class Task() {
  var inputs: java.util.Map[String, Array[Int]] = null

  def this(inputs: java.util.Map[String, Array[Int]]) = {
    this()
    this.inputs = inputs
  }
}