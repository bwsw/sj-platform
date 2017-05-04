package com.bwsw.sj.common.DAL.model.module

/**
 * Entity for task of execution plan
 *
 *
 * @author Kseniya Tomskikh
 */
class Task() {
  var inputs: java.util.Map[String, Array[Int]] = new java.util.HashMap()

  def addInput(name: String, partitions: Array[Int]): Array[Int] = {
    inputs.put(name, partitions)
  }
}