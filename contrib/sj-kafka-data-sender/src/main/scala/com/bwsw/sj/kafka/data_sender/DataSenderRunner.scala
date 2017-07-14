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

import scala.io.Source

/**
  * Main class for running [[DataSender]]
  *
  * Usage: {{{java [options] -jar `<`path-to-jar`>`}}}
  * e.g.:
  * {{{
  * java java -Daddress=localhost:9092 -Dwords=lorem,ipsum,dolor -Dsize=100 -Dcount=10 -jar sj-kafka-data-sender-1.0-SNAPSHOT.jar
  * }}}
  *
  * Options:
  * * address - address kafka server
  * * size - size of kafka message
  * * count - count of kafka messages
  * * words - available words (separated by ',')
  * * wordsFile - path to file with available words
  * * topic - name of kafka topic ("test" by default)
  * * separator - separator between words (' ' by default)
  *
  * Required: address, size, count; one of words or wordsFile.
  *
  * @author Pavel Tomskikh
  */
object DataSenderRunner extends App {
  private val defaultTopic = "test"
  private val defaultSeparator = " "

  private val address = Option(System.getProperty(PropertiesNames.address)).get
  private val topic = Option(System.getProperty(PropertiesNames.topic)).getOrElse(defaultTopic)
  private val separator = Option(System.getProperty(PropertiesNames.separator)).getOrElse(defaultSeparator)
  private val messageSize = Option(System.getProperty(PropertiesNames.messageSize)).get.toLong
  private val messageCount = Option(System.getProperty(PropertiesNames.messageCount)).get.toLong

  private val dataLoader = new DataSender(address, topic, getWords, separator)
  dataLoader.send(messageSize, messageCount)

  private def getWords: Seq[String] = {
    Option(System.getProperty(PropertiesNames.words)) match {
      case Some(words) =>
        words.split(",").toList

      case None =>
        val wordsFile = Option(System.getProperty(PropertiesNames.wordsFile)).get

        Source.fromFile(wordsFile).getLines().toList
    }
  }
}
