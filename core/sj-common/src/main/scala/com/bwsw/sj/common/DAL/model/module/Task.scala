package com.bwsw.sj.common.DAL.model.module

/**
 * Entity for task of execution plan
 *
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