package com.bwsw.sj.common.dal.repository

import com.mongodb.BasicDBObject
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Provides a service for access to elements of mongo database collection
  *
  * @tparam T type of collection elements
  */

class GenericMongoRepository[T: ClassTag](implicit injector: Injector) extends Repository[T] {

  import scala.collection.JavaConverters._

  /**
    * Allows manipulating with elements of mongo database collection
    */
  private val genericDAO = inject[ConnectionRepository].getGenericDAO[T]
  private val emptyQuery = new BasicDBObject()

  def save(entity: T): Unit = {
    logger.debug(s"Save an entity to a mongo database.")
    genericDAO.save(entity)
  }

  def get(name: String): Option[T] = {
    logger.debug(s"Retrieve an entity with name: '$name' from a mongo database.")
    Option(genericDAO.get(name))
  }

  def getByParameters(parameters: Map[String, Any]): Seq[T] = {
    logger.debug(s"Retrieve an entity from a mongo database by parameters: ${parameters.mkString(", ")}.")
    val query = genericDAO.createQuery().disableValidation()
    query.and(parameters.map(x => query.criteria(x._1).equal(x._2)).toSeq: _*)
    genericDAO.find(query).iterator().asScala.toSeq
  }

  def getAll: mutable.Buffer[T] = {
    logger.debug(s"Retrieve all entities from a mongo database.")
    genericDAO.find().asList().asScala
  }

  def delete(name: String): Unit = {
    logger.debug(s"Remove an entity with name: '$name' from a mongo database.")
    genericDAO.deleteById(name)
  }

  def deleteAll(): Unit = {
    logger.debug(s"Remove all entities from a mongo database.")
    genericDAO.getCollection.remove(emptyQuery)
  }
}