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

import com.bwsw.sj.common.si.model.stream.RestStream
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import scaldi.Injector

class RestStreamApi(name: String,
                    service: String,
                    tags: Option[Array[String]] = Some(Array()),
                    @JsonDeserialize(contentAs = classOf[Boolean]) force: Option[Boolean] = Some(false),
                    description: Option[String] = Some(RestLiterals.defaultDescription),
                    @JsonProperty("type") streamType: Option[String] = Some(StreamLiterals.restType),
                    creationDate: String)
  extends StreamApi(streamType.getOrElse(StreamLiterals.restType), name, service, tags, force, description, creationDate) {

  override def to(implicit injector: Injector): RestStream =
    new RestStream(
      name,
      service = service,
      tags = tags.getOrElse(Array()),
      force = force.getOrElse(false),
      streamType = streamType.getOrElse(StreamLiterals.restType),
      description = description.getOrElse(RestLiterals.defaultDescription),
      creationDate = creationDate)
}
