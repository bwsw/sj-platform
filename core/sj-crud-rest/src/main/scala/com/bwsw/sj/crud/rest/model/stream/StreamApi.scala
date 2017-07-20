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
package com.bwsw.sj.crud.rest.model.stream

import com.bwsw.sj.common.si.model.stream._
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injector

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[StreamApi], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[TStreamStreamApi], name = StreamLiterals.tstreamsType),
  new Type(value = classOf[KafkaStreamApi], name = StreamLiterals.kafkaType),
  new Type(value = classOf[ESStreamApi], name = StreamLiterals.elasticsearchType),
  new Type(value = classOf[JDBCStreamApi], name = StreamLiterals.jdbcType),
  new Type(value = classOf[RestStreamApi], name = StreamLiterals.restType)
))
class StreamApi(@JsonProperty("type") val streamType: String,
                val name: String,
                val service: String,
                val tags: Option[Array[String]] = Some(Array()),
                @JsonDeserialize(contentAs = classOf[Boolean]) val force: Option[Boolean] = Some(false),
                val description: Option[String] = Some(RestLiterals.defaultDescription),
                val creationDate: String) {

  @JsonIgnore
  def to(implicit injector: Injector): SjStream =
    new SjStream(
      streamType = streamType,
      name = name,
      service = service,
      tags = tags.getOrElse(Array()),
      force = force.getOrElse(false),
      description = description.getOrElse(RestLiterals.defaultDescription),
      creationDate = creationDate)
}

class StreamApiCreator {

  def from(stream: SjStream): StreamApi = stream.streamType match {
    case StreamLiterals.`tstreamsType` =>
      val tStreamStream = stream.asInstanceOf[TStreamStream]

      new TStreamStreamApi(
        name = tStreamStream.name,
        service = tStreamStream.service,
        tags = Option(tStreamStream.tags),
        force = Option(tStreamStream.force),
        description = Option(tStreamStream.description),
        partitions = Option(tStreamStream.partitions),
        creationDate = tStreamStream.creationDate
      )

    case StreamLiterals.`kafkaType` =>
      val kafkaStream = stream.asInstanceOf[KafkaStream]

      new KafkaStreamApi(
        name = kafkaStream.name,
        service = kafkaStream.service,
        tags = Option(kafkaStream.tags),
        force = Option(kafkaStream.force),
        description = Option(kafkaStream.description),
        partitions = Option(kafkaStream.partitions),
        replicationFactor = Option(kafkaStream.replicationFactor),
        creationDate = kafkaStream.creationDate
      )

    case StreamLiterals.`elasticsearchType` =>
      val esStream = stream.asInstanceOf[ESStream]

      new ESStreamApi(
        name = esStream.name,
        service = esStream.service,
        tags = Option(esStream.tags),
        force = Option(esStream.force),
        description = Option(esStream.description),
        creationDate = esStream.creationDate
      )

    case StreamLiterals.`restType` =>
      val restStream = stream.asInstanceOf[RestStream]

      new RestStreamApi(
        name = restStream.name,
        service = restStream.service,
        tags = Option(restStream.tags),
        force = Option(restStream.force),
        description = Option(restStream.description),
        creationDate = restStream.creationDate
      )

    case StreamLiterals.`jdbcType` =>
      val jdbcStream = stream.asInstanceOf[JDBCStream]

      new JDBCStreamApi(
        primary = jdbcStream.primary,
        name = jdbcStream.name,
        service = jdbcStream.service,
        tags = Option(jdbcStream.tags),
        force = Option(jdbcStream.force),
        description = Option(jdbcStream.description),
        creationDate = jdbcStream.creationDate
      )
  }
}
