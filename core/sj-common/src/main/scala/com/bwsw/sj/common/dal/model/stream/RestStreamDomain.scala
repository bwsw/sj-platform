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

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

/**
  * Stream for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestStreamDomain(override val name: String,
                       override val service: RestServiceDomain,
                       override val description: String = RestLiterals.defaultDescription,
                       override val force: Boolean = false,
                       override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.restType)
