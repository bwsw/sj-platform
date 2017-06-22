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

import java.sql.PreparedStatement

import com.bwsw.sj.common.engine.core.output.EntityBuilder
import com.bwsw.sj.engine.core.output.types.jdbc._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by diryavkin_dn on 07.03.17.
  */
class JdbcCommandBuilderTests extends FlatSpec with Matchers {
  it should "work properly" in {
    val jdbcMock = new JdbcMock()
    val eb = new EntityBuilder[(PreparedStatement, Int) => Unit]()

    val e = eb
      .field(new LongField("id"))
      .field(new JavaStringField("name"))
      .field(new IntegerField("age"))
      .field(new BooleanField("married", false))
      .build()

    val jdbccb = new JdbcCommandBuilder(jdbcMock, "txn", e)

    val data = Map[String, Object]().empty +
      ("id" -> new java.lang.Long(0)) +
      ("name" -> "John Smith") +
      ("age" -> new java.lang.Integer(32)) +
      ("married" -> new java.lang.Boolean(true))

    jdbccb.buildInsert(1, data).isInstanceOf[PreparedStatement] shouldBe true
  }

  it should "exists work" in {
    val jdbcMock = new JdbcMock()
    val eb = new EntityBuilder[(PreparedStatement, Int) => Unit]()

    val e = eb
      .field(new LongField("id"))
      .field(new JavaStringField("name"))
      .field(new IntegerField("age"))
      .field(new BooleanField("married", false))
      .build()

    val jdbccb = new JdbcCommandBuilder(jdbcMock, "txn", e)

    jdbccb.exists(1).isInstanceOf[PreparedStatement] shouldBe true
  }
}
