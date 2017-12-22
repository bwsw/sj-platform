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
package com.bwsw.sj.common.dal.model.service

import java.util.Date

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.utils.ServiceLiterals
import org.eclipse.jetty.http.{HttpScheme, HttpVersion}

/**
  * Service for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestServiceDomain(name: String,
                        description: String,
                        provider: ProviderDomain,
                        @PropertyField("base-path") val basePath: String,
                        @PropertyField("http-scheme") val httpScheme: HttpScheme,
                        @PropertyField("http-version") val httpVersion: HttpVersion,
                        val headers: java.util.Map[String, String],
                        creationDate: Date)
  extends ServiceDomain(name, description, provider, ServiceLiterals.restType, creationDate) {
}
