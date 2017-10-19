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
package com.bwsw.common.es

import com.bwsw.common.embedded.EmbeddedElasticsearch
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.QueryBuilders
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.util.Random

class ElasticsearchClientTestSuite extends FlatSpec with Matchers with BeforeAndAfterEach {
  val port = 9300
  val hosts = Set(("127.0.0.1", port))
  var embeddedElasticsearch: Option[EmbeddedElasticsearch] = None

  override protected def beforeEach(): Unit = {
    embeddedElasticsearch = Option(new EmbeddedElasticsearch(port))
    embeddedElasticsearch.foreach(_.start())
  }

  override protected def afterEach(): Unit = {
    embeddedElasticsearch.foreach(_.stop())
  }

  "isConnected" should "return true if client has connected to ES" in {
    //arrange and act
    val client = new ElasticsearchClient(hosts)

    //assert
    client.isConnected shouldBe true
  }

  "isConnected" should "return false if client has connected to ES but the node has been shutdown" in {
    //arrange and act
    val client = new ElasticsearchClient(hosts)
    embeddedElasticsearch.foreach(_.stop())
    //wait for closing a connection between server and client
    Thread.sleep(5000)

    //assert
    client.isConnected shouldBe false
  }

  "doesIndexExist" should "return false if an index doesn't exist" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"

    //assert
    client.doesIndexExist(index) shouldBe false
  }

  "Client" should "create an index if it doesn't exist" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"

    //act
    client.createIndex(index)

    //assert
    client.doesIndexExist(index) shouldBe true
  }

  "Client" should "not create an index if it already exists" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"

    //act
    client.createIndex(index)

    assertThrows[ResourceAlreadyExistsException] {
      client.createIndex(index)
    }
  }

  "deleteIndex" should "throw an exception if an index doesn't exist" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"

    //act and assert
    assertThrows[IndexNotFoundException] {
      client.deleteIndex(index)
    }
  }

  "Client" should "delete the existing index" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"
    client.createIndex(index)

    //act
    client.deleteIndex(index)

    //assert
    client.doesIndexExist(index) shouldBe false
  }

  "Client" should "write a document" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"
    val documentType = "test"
    client.createIndex(index)

    val data = """ {"test_filed":"test_value"} """

    //act
    client.write(data, index, documentType)
    //wait for a write operation completed
    Thread.sleep(1000)

    //assert
    client.search(index, documentType).getTotalHits shouldBe 1
  }

  "Client" should "write the several documents" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"
    val documentType = "test"
    client.createIndex(index)

    val numberOfDocuments = 5
    val data = """ {"test_filed":"test_value"} """

    //act
    (0 until numberOfDocuments).foreach(_ => {
      client.write(data, index, documentType)
      //wait for a write operation completed
      Thread.sleep(1000)
    })

    //assert
    client.search(index, documentType).getTotalHits shouldBe numberOfDocuments
  }

  "Client" should "delete all documents that has been written if no match query has been passed" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"
    val documentType = "test"
    client.createIndex(index)
    val testField = "test_filed"
    val testValue = "test_value"
    val numberOfDocuments = 5
    val data = s""" {"$testField":"$testValue"} """
    (0 until numberOfDocuments).foreach(_ => {
      client.write(data, index, documentType)
      //wait for a write operation completed
      Thread.sleep(1000)
    })

    //act
    client.deleteDocuments(index, documentType)
    //wait for a delete operation completed
    Thread.sleep(1000)

    //assert
    client.search(index, documentType).getTotalHits shouldBe 0
  }

  "Client" should "delete documents according to passed match query" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"
    val documentType = "test"
    client.createIndex(index)
    val testField = "test_field"
    val testValue = "test_value"
    val numberOfDocuments = 5
    (0 until numberOfDocuments).foreach(x => {
      val data = s""" {"${testField + x}":"$testValue"} """
      client.write(data, index, documentType)
      //wait for a write operation completed
      Thread.sleep(1000)
    })

    //act
    val s = testField + Random.nextInt(numberOfDocuments)
    client.deleteDocuments(index, documentType, QueryBuilders.matchQuery(s, testValue).toString)
    //wait for a delete operation completed
    Thread.sleep(1000)

    //assert
    client.search(index, documentType).getTotalHits shouldBe (numberOfDocuments - 1)
  }

  "deleteDocuments" should "throw an exception if non-existing index has been passed" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"
    val documentType = "test"

    //act and assert
    assertThrows[IndexNotFoundException](
      client.deleteDocuments(index, documentType)
    )
  }

  "deleteDocuments" should "not delete the documents if an incorrect document type has been passed" in {
    //arrange
    val client = new ElasticsearchClient(hosts)
    val index = "test_index"
    val documentType = "test"
    client.createIndex(index)
    val testField = "test_filed"
    val testValue = "test_value"
    val numberOfDocuments = 5
    val data = s""" {"$testField":"$testValue"} """
    (0 until numberOfDocuments).foreach(_ => {
      client.write(data, index, documentType)
      //wait for a write operation completed
      Thread.sleep(1000)
    })

    //act
    client.deleteDocuments(index, documentType + "dummy")
    //wait for a delete operation completed
    Thread.sleep(1000)

    //assert
    client.search(index, documentType).getTotalHits shouldBe numberOfDocuments
  }
}
