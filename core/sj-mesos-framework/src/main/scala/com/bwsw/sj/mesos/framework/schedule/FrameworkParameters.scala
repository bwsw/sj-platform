/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bwsw.sj.mesos.framework.schedule

import com.bwsw.sj.common.utils.{CommonAppConfigNames, FrameworkLiterals}
import com.typesafe.config.ConfigFactory

import scala.collection.immutable
import scala.util.Try

/**
  * Environment variables store
  */
object FrameworkParameters {
  val instanceId = "instanceId"
  val mongoHosts = "mongodbHosts"
  val mognoUser = "mongodbUser"
  val mongoPassword = "mongoPassword"

  private var params: Map[String, String] = immutable.Map[String, String]()

  def prepareEnvParams() = {
    val config = ConfigFactory.load()

    this.params = Map(
      instanceId ->
        Try(config.getString(FrameworkLiterals.instanceId)).getOrElse("00000000-0000-0000-0000-000000000000"),
      mongoHosts -> Try(config.getString(CommonAppConfigNames.mongoHosts)).getOrElse("127.0.0.1:27017"),
      mognoUser -> Try(config.getString(CommonAppConfigNames.mongoUser)).getOrElse("user"),
      mongoPassword -> Try(config.getString(CommonAppConfigNames.mongoPassword)).getOrElse("password")
    )
  }

  def apply(parameterName: String) = {
    params(parameterName)
  }

  def apply(): Map[String, String] = {
    params
  }


}
