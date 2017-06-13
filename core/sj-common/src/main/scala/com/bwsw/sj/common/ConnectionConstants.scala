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
package com.bwsw.sj.common

import com.bwsw.sj.common.utils.CommonAppConfigNames
import com.typesafe.config.ConfigFactory

import scala.util.Try

object ConnectionConstants {
  private val config = ConfigFactory.load()
  val mongoHosts: String = config.getString(CommonAppConfigNames.mongoHosts)
  val databaseName: String = config.getString(CommonAppConfigNames.mongoDbName)
  val mongoUser: Option[String] = Try(config.getString(CommonAppConfigNames.mongoUser)).toOption
  val mongoPassword: Option[String] = Try(config.getString(CommonAppConfigNames.mongoPassword)).toOption
}