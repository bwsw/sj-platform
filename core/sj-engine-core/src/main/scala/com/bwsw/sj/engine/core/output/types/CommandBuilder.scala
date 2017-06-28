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