package com.bwsw.sj.common.DAL.service

import com.bwsw.sj.common.DAL.repository.ConnectionRepository

import scala.reflect.ClassTag

/**
 * Provides a service for access to elements of mongo database collection
 * @tparam T Type of collection elements
 */

class GenericMongoService[T: ClassTag] extends DatabaseService[T] {

  import scala.collection.JavaConverters._

  /**
   * Allows manipulating with elements of mongo database collection
   */
  private val genericDAO = ConnectionRepository.getGenericDAO[T]

  /**
   * Allows adding new element or updating an element
   * @param entity Specific element of T type
   */
  def save(entity: T) = {
    logger.debug(s"Save an entity to a mongo database")
    genericDAO.save(entity)
  }

  /**
   * Allows retrieving an element by name (id)
   * @param name Id of element
   * @return Specific element of T type
   */
  def get(name: String) = {
    logger.debug(s"Retrieve an entity with name: '$name' from a mongo database")
    genericDAO.get(name)
  }

  /**
   * Allows retrieving an element by set of fields
   * @param parameters Set of fields of element (name of element field -> value of element field)
   * @return Set of elements matching the parameters
   */
  def getByParameters(parameters: Map[String, Any]) = {
    logger.debug(s"Retrieve an entity from a mongo database by parameters: ${parameters.mkString(", ")}")
    val query = genericDAO.createQuery()
    query.and(parameters.map(x => query.criteria(x._1).equal(x._2)).toSeq: _*)
    query.asList().asScala
  }

  /**
   * Allows retrieving all elements from collection
   * @return Set of elements
   */
  def getAll = {
    logger.debug(s"Retrieve all entities from a mongo database")
    genericDAO.find().asList().asScala
  }

  /**
   * Allows deleting an element by name (id)
   * @param name Id of element
   */
  def delete(name: String) = {
    logger.debug(s"Remove an entity with name: '$name' from a mongo database")
    genericDAO.deleteById(name)
  }

  /**
   * Allows deleting all elements from collection
   */
  def deleteAll() = {
    logger.debug(s"Remove all entities from a mongo database")
    genericDAO.getCollection.drop()
  }
}