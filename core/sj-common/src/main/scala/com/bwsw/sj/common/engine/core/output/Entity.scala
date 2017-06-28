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
package com.bwsw.sj.common.engine.core.output

import com.bwsw.sj.common.utils.EngineLiterals

/**
  * User defined class to keep processed data of [[EngineLiterals.outputStreamingType]] module
  *
  * @param m set of field names and their values
  * @tparam T type of data elements
  * @author Ivan Kudryavtsev
  */

class Entity[T](m: Map[String, NamedType[T]]) {
  def getField(name: String): NamedType[T] = m(name)

  def getFields: Iterable[String] = m.keys
}

/**
  * Build an entity [[Entity]] by setting fields one by one (for convenience)
  *
  * {{{
  * val entityBuilder = new ElasticsearchEntityBuilder()
  * val entity = entityBuilder
  * .field(new DateField("test-date"))
  * .field(new IntegerField("value"))
  * .field(new JavaStringField("string-value"))
  * .build()
  * }}}
  *
  * @param m set of field names and their values
  * @tparam T type of data elements
  * @author Ivan Kudryavtsev
  */
class EntityBuilder[T](m: Map[String, NamedType[T]] = Map[String, NamedType[T]]()) {

  def build(): Entity[T] = new Entity[T](m)

  def field(c: NamedType[T]): EntityBuilder[T] = {
    new EntityBuilder[T](m + (c.getName -> c))
  }
}