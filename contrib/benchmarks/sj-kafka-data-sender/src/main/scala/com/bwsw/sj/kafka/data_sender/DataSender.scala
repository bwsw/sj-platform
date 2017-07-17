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
package com.bwsw.sj.kafka.data_sender

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.Random

/**
  * Choose random words, concatenates them, and sends to a kafka server
  *
  * @param address   address a kafka server
  * @param topic     name of topic
  * @param words     available words
  * @param separator separator between words
  * @author Pavel Tomskikh
  */
class DataSender(address: String, topic: String, words: Seq[String], separator: String) {
  require(separator.length == 1, "separator must contain exactly one symbol")

  private val stringSerializerClass = "org.apache.kafka.common.serialization.StringSerializer"
  private val producerProps = new Properties
  producerProps.put("bootstrap.servers", address)
  producerProps.put("key.serializer", stringSerializerClass)
  producerProps.put("value.serializer", stringSerializerClass)

  /**
    * Generates data and send it to a kafka server
    *
    * @param messageSize size of one message
    * @param messages    count of messages
    */
  def send(messageSize: Long, messages: Long): Unit = {
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
}
