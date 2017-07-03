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
package com.bwsw.common

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

import scala.util.parsing.json.JSON

/**
  * Tests for [[JsonSerializer]]
  *
  * @author Pavel Tomskikh
  */
class JsonSerializerTests extends FlatSpec with Matchers with TableDrivenPropertyChecks {

  import JsonSerializerTests._

  val flags = Seq(true, false)

  val innerObjects = Seq(
    InnerObject(1, "test"),
    InnerObject(2, "test2"),
    InnerObject(44, "test44"),
    InnerObject(55, "test55"))

  val outerObjects = Seq(
    new OuterObject("t", 1, InnerObject(1, "test1")),
    new OuterObjectType1(2, InnerObject(2, "test2"), true),
    new OuterObjectType1(3, InnerObject(3, "test3"), false),
    new OuterObjectType2(4, InnerObject(4, "test4"), "field4-value4"),
    new OuterObjectType2(5, InnerObject(5, "test5"), "field4-value5"),
    new OuterObjectType2(6, InnerObject(6, "test5"), "field4-value6"))

  "JsonSerializer" should "return FAIL_ON_UNKNOWN_PROPERTIES flag properly" in {
    flags.foreach { flag =>
      val serializer = new JsonSerializer

      serializer.setIgnoreUnknown(flag)
      serializer.getIgnoreUnknown shouldBe flag
    }
  }

  it should "return FAIL_ON_NULL_FOR_PRIMITIVES flag properly" in {
    flags.foreach { flag =>
      val serializer = new JsonSerializer

      serializer.disableNullForPrimitives(flag)
      serializer.nullForPrimitivesIsDisabled shouldBe flag
    }
  }

  it should "serialize objects properly" in {
    val serializer = new JsonSerializer
    (innerObjects ++ outerObjects).foreach { obj =>
      val json = serializer.serialize(obj)

      JSON.parseFull(json) shouldBe Some(obj.toMap)
    }
  }

  it should "deserialize simple objects properly" in {
    val serializer = new JsonSerializer
    innerObjects.foreach { obj =>
      serializer.deserialize[InnerObject](obj.toJson) shouldBe obj
    }
  }

  it should "deserialize object with different type properly" in {
    val serializer = new JsonSerializer
    outerObjects.foreach { obj =>
      serializer.deserialize[OuterObject](obj.toJson) shouldBe obj
    }
  }
}

object JsonSerializerTests {

  val typeField = "type"
  val field1Name = "field1"
  val field2Name = "field2"
  val field3Name = "field3"
  val field4Name = "field4"
  val field5Name = "field5"

  val type1 = "type1"
  val type2 = "type2"

  trait ObjectInterface {
    def toMap: Map[String, Any]

    def toJson: String = mapToJson(toMap)

  }

  case class InnerObject(field4: Int, field5: String) extends ObjectInterface {
    override def toMap: Map[String, Any] = Map(
      field4Name -> field4,
      field5Name -> field5)
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[OuterObject], visible = true)
  @JsonSubTypes(Array(
    new Type(value = classOf[OuterObjectType1], name = "type1"),
    new Type(value = classOf[OuterObjectType2], name = "type2")))
  class OuterObject(val `type`: String, val field1: Int, val field2: InnerObject) extends ObjectInterface {
    override def toMap: Map[String, Any] = Map(
      typeField -> `type`,
      field1Name -> field1,
      field2Name -> field2.toMap)

    override def equals(obj: scala.Any): Boolean = obj match {
      case o: OuterObject => o.toMap == toMap
      case _ => super.equals(obj)
    }
  }

  class OuterObjectType1(field1: Int, field2: InnerObject, val field3: Boolean)
    extends OuterObject(type1, field1, field2) {

    override def toMap: Map[String, Any] =
      super.toMap + (field3Name -> field3)
  }

  class OuterObjectType2(field1: Int, field2: InnerObject, val field3: String)
    extends OuterObject(type2, field1, field2) {

    override def toMap: Map[String, Any] =
      super.toMap + (field3Name -> field3)
  }

  def mapToJson(map: Map[String, Any]): String =
    new ObjectMapper().registerModule(DefaultScalaModule).writeValueAsString(map)

}
