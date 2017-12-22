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
package com.bwsw.sj.common.si.model.stream

import java.util.Date

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.dal.model.stream.RestStreamDomain
import scaldi.Injector

class RestStream(name: String,
                 service: String,
                 tags: Array[String],
                 force: Boolean,
                 streamType: String,
                 description: String,
                 creationDate: String)
                (implicit injector: Injector)
  extends SjStream(streamType, name, service, tags, force, description, creationDate) {

  override def to(): RestStreamDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new RestStreamDomain(
      name,
      service = serviceRepository.get(service).get.asInstanceOf[RestServiceDomain],
      description = description,
      force = force,
      tags = tags,
      creationDate = new Date())
  }
}
