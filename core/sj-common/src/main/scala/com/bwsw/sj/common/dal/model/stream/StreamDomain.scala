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
package com.bwsw.sj.common.dal.model.stream

import java.util.Date

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField, ReferenceField}
import org.mongodb.morphia.annotations._

@Entity("streams")
class StreamDomain(@IdField val name: String,
                   val description: String,
                   @ReferenceField val service: ServiceDomain,
                   val force: Boolean,
                   val tags: Array[String],
                   @PropertyField("stream-type") val streamType: String,
                   val creationDate: Date) {

  def create(): Unit = {}
}
