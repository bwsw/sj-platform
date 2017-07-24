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

import com.bwsw.common.exceptions.{JsonDeserializationException, JsonIncorrectValueException, JsonUnrecognizedPropertyException}
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
    new OuterObject("other-type", 1, InnerObject(1, "test1")),
    new OuterObjectType1(2, InnerObject(2, "test2"), true),
    new OuterObjectType1(3, InnerObject(3, "test3"), false),
    new OuterObjectType2(4, InnerObject(4, "test4"), "field4-value4"),
    new OuterObjectType2(5, InnerObject(5, "test5"), "field4-value5"),
    new OuterObjectType2(6, InnerObject(6, "test5"), "field4-value6"),
    new OuterObject("other-type", 7, InnerObject(1, "test1")))

  "JsonSerializer" should "return 'ignoreUnknown' flag properly" in {
    flags.foreach { flag =>
      val serializer = new JsonSerializer

      serializer.setIgnoreUnknown(flag)
      serializer.getIgnoreUnknown shouldBe flag
    }
  }

  it should "return 'enableNullForPrimitives' flag properly" in {
    flags.foreach { flag =>
      val serializer = new JsonSerializer

      serializer.enableNullForPrimitives(flag)
      serializer.nullForPrimitivesIsEnabled shouldBe flag
    }
  }

  it should "serialize nested objects properly" in {
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

  it should "deserialize objects of different types properly" in {
    val serializer = new JsonSerializer
    outerObjects.foreach { obj =>
      serializer.deserialize[OuterObject](obj.toJson) shouldBe obj
    }
  }

  it should "deserialize json with missed fields when 'enableNullForPrimitives' flag is set" in {
    val someType = "some-type"
    val field1Value = 123
    val field4Value = 456
    val field5Value = "field 5 value"
    val field2Value = InnerObject(field4Value, field5Value)
    val objects = Table(
      ("obj", "missedField", "expectedObject"),
      (new OuterObject(someType, field1Value, field2Value), typeField, new OuterObject(null, field1Value, field2Value)),
      (new OuterObject(someType, field1Value, field2Value), field1Name, new OuterObject(someType, 0, field2Value)),
      (new OuterObject(someType, field1Value, field2Value), field2Name, new OuterObject(someType, field1Value, null)),
      (new OuterObject(someType, field1Value, field2Value), field4Name,
        new OuterObject(someType, field1Value, InnerObject(0, field5Value))),
      (new OuterObject(someType, field1Value, field2Value), field5Name,
        new OuterObject(someType, field1Value, InnerObject(field4Value, null))),
      (new OuterObjectType1(field1Value, field2Value, true), field3Name,
        new OuterObjectType1(field1Value, field2Value, false)),
      (new OuterObjectType2(field1Value, field2Value, "field 3 value"), field3Name,
        new OuterObjectType2(field1Value, field2Value, null)))

    val serializer = new JsonSerializer(enableNullForPrimitives = true)

    forAll(objects) { (obj, missedField, expectedObject) =>
      val json = mapToJson(removeFieldFromMap(obj.toMap, missedField))
      serializer.deserialize[OuterObject](json) shouldBe expectedObject
    }
  }

  it should "not deserialize json without some primitive values when 'disableNullForPrimitives' flag isn't set" in {
    val obj = new OuterObjectType1(123, InnerObject(456, "field 5 value"), true)
    val missedFields = Seq(field1Name, field3Name, field4Name)
    val serializer = new JsonSerializer(enableNullForPrimitives = false)
    missedFields.foreach { missedField =>
      val json = mapToJson(removeFieldFromMap(obj.toMap, missedField))
      a[JsonIncorrectValueException] shouldBe thrownBy {
        serializer.deserialize[OuterObject](json)
      }
    }
  }

  it should "deserialize json with unknown fields when 'ignoreUnknown' flag is set" in {
    val obj = new OuterObjectType1(123, InnerObject(456, "field 5 value"), true)
    val unknownField = "unknown-field"
    val json = mapToJson(obj.toMap + (unknownField -> "value"))
    val serializer = new JsonSerializer(ignoreUnknown = true)
    serializer.deserialize[OuterObject](json) shouldBe obj
  }

  it should "not deserialize json with unknown fields when 'ignoreUnknown' flag isn't set" in {
    val obj = new OuterObjectType1(123, InnerObject(456, "field 5 value"), true)
    val unknownField = "unknown-field"
    val json = mapToJson(obj.toMap + (unknownField -> "value"))
    val serializer = new JsonSerializer(ignoreUnknown = false)
    val thrown = the[JsonUnrecognizedPropertyException] thrownBy {
      serializer.deserialize[OuterObject](json)
    }

    thrown.getMessage should include(unknownField)
  }

  it should "not deserialize json if a field contains a value of incorrect type" in {
    val obj = new OuterObjectType1(123, InnerObject(456, "field 5 value"), true)
    val map = obj.toMap
    val incorrectValues = Table(
      ("field", "value"),
      (field1Name, "wrong type"),
      (field2Name, "wrong type"),
      (field3Name, "wrong type"),
      (field4Name, "wrong type"),
      (field5Name, Array()))

    val serializer = new JsonSerializer(ignoreUnknown = true)
    forAll(incorrectValues) { (field, value) =>
      val json = mapToJson(replaceFieldValue(map, field, value))
      val thrown = the[JsonIncorrectValueException] thrownBy {
        serializer.deserialize[OuterObject](json)
      }
      thrown.getMessage should include(field)
    }
  }

  it should "throw JsonDeserializationException for incorrect json" in {
    val incorrectJsons = Seq(
      null,
      "",
      """{"a":1,}""")

    val serializer = new JsonSerializer()
    incorrectJsons.foreach { incorrectJson =>
      a[JsonDeserializationException] shouldBe thrownBy {
        serializer.deserialize(incorrectJson)
      }
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
      field1Name -> field1) ++ {
      if (field2 != null) Map(field2Name -> field2.toMap)
      else Map.empty
    }

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

  def removeFieldFromMap(map: Map[String, Any], field: String): Map[String, Any] = field match {
    case `field4Name` | `field5Name` =>
      val field2Value = map(field2Name).asInstanceOf[Map[String, Any]] - field
      map + (field2Name -> field2Value)
    case _ =>
      map - field
  }

  def replaceFieldValue(map: Map[String, Any], field: String, value: Any): Map[String, Any] = field match {
    case `field4Name` | `field5Name` =>
      val field2Value = map(field2Name).asInstanceOf[Map[String, Any]] + (field -> value)
      map + (field2Name -> field2Value)
    case _ =>
      map + (field -> value)
  }
}
