package com.bwsw.sj.common.dal.repository

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/**
  * Provides methods for access to a database collection
  *
  * @tparam T type of collection elements
  */
trait Repository[T] {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Allows adding new element or updating an element
    *
    * @param entity specific element of T type
    */
  def save(entity: T): Unit

  /**
    * Allows retrieving an element by name (id)
    *
    * @param name id of element
    * @return specific element of T type
    */
  def get(name: String): Option[T]

  /**
    * Allows retrieving an element by set of fields
    *
    * @param parameters set of fields of element (name of element field -> value of element field)
    * @return set of elements matching the parameters
    */
  def getByParameters(parameters: Map[String, Any]): mutable.Buffer[T]

  /**
    * Allows retrieving all elements from collection
    *
    * @return set of elements
    */
  def getAll: mutable.Buffer[T]

  /**
    * Allows deleting an element by name (id)
    *
    * @param name id of element
    */
  def delete(name: String): Unit

  /**
    * Allows deleting all elements from collection
    */
  def deleteAll(): Unit
}
