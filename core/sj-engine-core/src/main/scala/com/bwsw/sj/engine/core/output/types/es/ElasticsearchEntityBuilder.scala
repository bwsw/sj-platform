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
package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.sj.common.engine.core.output.{EntityBuilder, NamedType}
import com.bwsw.sj.engine.core.output.IncompatibleTypeException

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchEntityBuilder(m: Map[String, NamedType[String]] = Map[String, NamedType[String]]()) extends EntityBuilder[String](m) {
  def field[DV](c: ElasticsearchField[DV]): ElasticsearchEntityBuilder = {
    new ElasticsearchEntityBuilder(m + (c.getName -> c.asInstanceOf[NamedType[String]]))
  }

  override def field(c: NamedType[String]): EntityBuilder[String] = throw new IncompatibleTypeException("Use a more specific method field. Parent method is locked.")
}
