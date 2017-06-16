package com.bwsw.sj.engine.core.output.types

/**
  * Common trait for building queries to CRUD data
  *
  * @tparam T type of built query
  * @author Pavel Tomskikh
  */
trait CommandBuilder[T] {

  /**
    * Builds query for data insertion
    *
    * @param transaction transaction ID
    * @param values      data
    * @return insertion query
    */
  def buildInsert(transaction: Long, values: Map[String, Any]): T

  /**
    * Builds query for data deletion
    *
    * @param transaction transaction ID
    * @return deletion query
    */
  def buildDelete(transaction: Long): T
}