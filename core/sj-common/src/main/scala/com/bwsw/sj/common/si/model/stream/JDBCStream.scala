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

import com.bwsw.sj.common.dal.model.service.JDBCServiceDomain
import com.bwsw.sj.common.dal.model.stream.JDBCStreamDomain
import com.bwsw.sj.common.rest.utils.ValidationUtils.isAlphaNumericWithUnderscore
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class JDBCStream(name: String,
                 service: String,
                 val primary: String,
                 tags: Array[String],
                 force: Boolean,
                 streamType: String,
                 description: String,
                 creationDate: String)
                (implicit injector: Injector)
  extends SjStream(streamType, name, service, tags, force, description, creationDate) {

  import messageResourceUtils.createMessage

  override def to(): JDBCStreamDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new JDBCStreamDomain(
      name,
      service = serviceRepository.get(service).get.asInstanceOf[JDBCServiceDomain],
      primary = primary,
      description = description,
      force = force,
      tags = tags,
      creationDate = new Date())
  }

  //it is necessary to work with PostgreSQL that doesn't allow to use hyphens in the table name
  override protected def validateStreamName(name: String): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    if (!isAlphaNumericWithUnderscore(name)) {
      errors += createMessage("entity.error.jdbc.incorrect.stream.name", name)
    }

    errors
  }
}
