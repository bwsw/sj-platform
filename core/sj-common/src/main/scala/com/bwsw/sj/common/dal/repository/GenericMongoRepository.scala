/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.dal.repository

import com.mongodb.BasicDBObject

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Provides a service for access to elements of mongo database collection
  *
  * @tparam T type of collection elements
  */

class GenericMongoRepository[T: ClassTag](connectionRepository: ConnectionRepository) extends Repository[T] {

  import scala.collection.JavaConverters._

  /**
    * Allows manipulating with elements of mongo database collection
    */
  private val genericDAO = connectionRepository.getGenericDAO[T]
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