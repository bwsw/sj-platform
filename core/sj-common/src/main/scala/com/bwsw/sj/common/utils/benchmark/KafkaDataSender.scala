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
package com.bwsw.sj.common.utils.benchmark

import java.util.Properties

import com.bwsw.common.ObjectSerializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.Random

/**
  * Sends data to a kafka server
  *
  * @param address address a kafka server
  * @author Pavel Tomskikh
  */
class KafkaDataSender(address: String) {
  private val producerProps = new Properties
  producerProps.put("bootstrap.servers", address)

  /**
    * Generates random words, concatenates them and sends into Kafka topic
    *
    * @param messageSize size of one message
    * @param messages    count of messages
    * @param words       available words
    * @param separator   separator between words
    * @param topic       Kafka topic
    */
  def send(messageSize: Long, messages: Long, words: Seq[String], separator: String, topic: String): Unit = {
    require(separator.length == 1, "separator must contain exactly one symbol")

    val stringSerializerClass = "org.apache.kafka.common.serialization.StringSerializer"
    producerProps.put("key.serializer", stringSerializerClass)
    producerProps.put("value.serializer", stringSerializerClass)
    val producer = new KafkaProducer[String, String](producerProps)

    (0l until messages).foreach { _ =>
      var message = words(Random.nextInt(words.length))
      while (message.getBytes.length < messageSize)
        message += separator + words(Random.nextInt(words.length))

      val record = new ProducerRecord[String, String](topic, message)
      producer.send(record)
    }

    producer.close()
  }

  /**
    * Sends data into Kafka topic
    *
    * @param elements data
    * @param topic    Kafka topic
    */
  def send(elements: Iterable[AnyRef], topic: String): Unit = {
    val byteArraySerializerClass = "org.apache.kafka.common.serialization.ByteArraySerializer"
    producerProps.put("key.serializer", byteArraySerializerClass)
    producerProps.put("value.serializer", byteArraySerializerClass)
    val producer = new KafkaProducer[Array[Byte], Array[Byte]](producerProps)
    val objectSerializer = new ObjectSerializer

    elements
      .map(objectSerializer.serialize)
      .map(bytes => new ProducerRecord[Array[Byte], Array[Byte]](topic, bytes))
      .foreach(producer.send)

    producer.close()
  }
}
