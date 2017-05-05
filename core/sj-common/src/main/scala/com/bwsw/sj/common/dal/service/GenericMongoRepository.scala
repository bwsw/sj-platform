package com.bwsw.sj.common.dal.service

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.mongodb.BasicDBObject

import scala.reflect.ClassTag

/**
 * Provides a service for access to elements of mongo database collection
 * @tparam T Type of collection elements
 */

class GenericMongoRepository[T: ClassTag] extends Repository[T] {

  import scala.collection.JavaConverters._

  /**
   * Allows manipulating with elements of mongo database collection
   */
  private val genericDAO = ConnectionRepository.getGenericDAO[T]
  private val emptyQuery = new BasicDBObject()

  def save(entity: T) = {
    logger.debug(s"Save an entity to a mongo database.")
    genericDAO.save(entity)
  }

  def get(name: String) = {
    logger.debug(s"Retrieve an entity with name: '$name' from a mongo database.")
    Option(genericDAO.get(name))
  }

  def getByParameters(parameters: Map[String, Any]) = {
    logger.debug(s"Retrieve an entity from a mongo database by parameters: ${parameters.mkString(", ")}.")
    val query = genericDAO.createQuery()
    query.and(parameters.map(x => query.criteria(x._1).equal(x._2)).toSeq: _*)
    query.asList().asScala
  }

  def getAll = {
    logger.debug(s"Retrieve all entities from a mongo database.")
    genericDAO.find().asList().asScala
  }

  def delete(name: String) = {
    logger.debug(s"Remove an entity with name: '$name' from a mongo database.")
    genericDAO.deleteById(name)
  }

  def deleteAll() = {
    logger.debug(s"Remove all entities from a mongo database.")
    genericDAO.getCollection.remove(emptyQuery)
  }
}