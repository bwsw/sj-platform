package com.bwsw.sj.common.DAL

import scala.reflect.ClassTag

class GenericMongoService[T: ClassTag] extends DatabaseService[T]{

  import scala.collection.JavaConverters._

  private val genericDAO = ConnectionRepository.getGenericDAO[T]

  def save(entity: T) = {
    genericDAO.save(entity)
  }

  def get(name: String) = {
    genericDAO.get(name)
  }

  def getByParameters(parameters: Map[String, Any]) = {
    val query = genericDAO.createQuery()
    query.and(parameters.map(x => query.criteria(x._1).equal(x._2)).toSeq: _*)
    query.asList().asScala
  }

  def getAll = {
    genericDAO.find().asList().asScala
  }

  def delete(name: String) = {
    genericDAO.deleteById(name)
  }

  def deleteAll() = {
    genericDAO.getCollection.drop()
  }
}