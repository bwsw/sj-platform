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
package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.common.si.result.{CreationResult, DeletionResult}

/**
  * Provides methods to access entities in [[com.bwsw.sj.common.dal.repository.GenericMongoRepository]]
  *
  * @tparam M type of entities
  * @tparam T type of domain entities, storing in [[com.bwsw.sj.common.dal.repository.GenericMongoRepository]]
  */
trait ServiceInterface[M, T] {
  protected val entityRepository: GenericMongoRepository[T]

  /**
    * Saves entity to [[entityRepository]]
    *
    * @param entity
    */
  def create(entity: M): CreationResult

  def getAll(): Seq[M]

  def get(name: String): Option[M]

  /**
    * Deletes entity from [[entityRepository]] by name
    *
    * @param name name of entity
    */
  def delete(name: String): DeletionResult
}
