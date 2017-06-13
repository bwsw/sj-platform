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
package com.bwsw.sj.engine.core.output.types.jdbc

import com.bwsw.sj.engine.core.output.types.jdbc._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by diryavkin_dn on 06.03.17.
  */
class JdbcEntityBuilderTests extends FlatSpec with Matchers {
  it should "work properly" in {
    val jdbcBuilder = new JdbcEntityBuilder()

    val age = new IntegerField("age")

    val jdbcEntity = jdbcBuilder.field(age).build()

    jdbcEntity.getField("age").isInstanceOf[IntegerField] shouldBe true

  }
}
