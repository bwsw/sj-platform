package com.bwsw.sj.common.DAL.service

import scala.collection.mutable

/**
 * Provides methods for access to a database collection
 * @tparam T Type of collection elements
 */
trait DatabaseService[T] {

  /**
   * Allows adding new element or updating an element
   * @param entity Specific element of T type
   */
  def save(entity: T)

  /**
   * Allows retrieving an element by name (id)
   * @param name Id of element
   * @return Specific element of T type
   */
  def get(name: String): T

  /**
   * Allows retrieving an element by set of fields
   * @param parameters Set of fields of element (name of element field -> value of element field)
   * @return Set of elements matching the parameters
   */
  def getByParameters(parameters: Map[String, Any]): mutable.Buffer[T]

  /**
   * Allows retrieving all elements from collection
   * @return Set of elements
   */
  def getAll: mutable.Buffer[T]

  /**
   * Allows deleting an element by name (id)
   * @param name Id of element
   */
  def delete(name: String)

  /**
   * Allows deleting all elements from collection
   */
  def deleteAll()
}
