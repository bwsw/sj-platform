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

import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * Provides methods for access to a database collection
  *
  * @tparam T type of collection elements
  */
trait Repository[T] {

  protected val logger: Logger = Logger(this.getClass)

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
  def getByParameters(parameters: Map[String, Any]): Seq[T]

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
