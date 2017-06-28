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
package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.engine.core.output.EntityBuilder
import com.bwsw.sj.engine.core.output.types.es.{BooleanField, IntegerField, JavaStringField, LongField}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class EntityBuilderTests extends FlatSpec with Matchers {
  it should "work properly" in {
    val eb = new EntityBuilder[String]()

    val id = new LongField("id")
    val name = new JavaStringField("name")
    val age = new IntegerField("age")
    val married = new BooleanField("married", false)

    val e = eb.field(id).field(name).field(age).field(married).build()

    e.getField("id").isInstanceOf[LongField] shouldBe true
    e.getField("name").isInstanceOf[JavaStringField] shouldBe true
    e.getField("age").isInstanceOf[IntegerField] shouldBe true
    e.getField("married").isInstanceOf[BooleanField] shouldBe true

    e.getField("id").asInstanceOf[LongField] shouldBe id
    e.getField("name").asInstanceOf[JavaStringField] shouldBe name
    e.getField("age").asInstanceOf[IntegerField] shouldBe age
    e.getField("married").asInstanceOf[BooleanField] shouldBe married

  }
}
