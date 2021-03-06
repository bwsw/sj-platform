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

import com.bwsw.sj.common.si.model.stream.TStreamStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injector

class TStreamStreamApi(name: String,
                       service: String,
                       tags: Option[Array[String]] = Some(Array()),
                       @JsonDeserialize(contentAs = classOf[Boolean]) force: Option[Boolean] = Some(false),
                       description: Option[String] = Some(RestLiterals.defaultDescription),
                       @JsonDeserialize(contentAs = classOf[Int]) val partitions: Option[Int] = Some(Int.MinValue),
                       @JsonProperty("type") streamType: Option[String] = Some(StreamLiterals.tstreamsType),
                       creationDate: String)
  extends StreamApi(streamType.getOrElse(StreamLiterals.tstreamsType), name, service, tags, force, description, creationDate) {

  override def to(implicit injector: Injector): TStreamStream =
    new TStreamStream(
      name,
      service,
      partitions.getOrElse(Int.MinValue),
      tags.getOrElse(Array()),
      force.getOrElse(false),
      streamType.getOrElse(StreamLiterals.tstreamsType),
      description.getOrElse(RestLiterals.defaultDescription),
      creationDate = creationDate)
}
