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
package com.bwsw.common.embedded

import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version

/**
  * Used for testing purposes only. Mongo DB server.
  *
  * @param port mongo port
  * @author Pavel Tomskikh
  */
class EmbeddedMongo(port: Int) {
  private val config = new MongodConfigBuilder().net(new Net(port, false)).version(Version.V3_5_1).build()
  private val mongodStarter = MongodStarter.getDefaultInstance
  private val mongo = mongodStarter.prepare(config)

  def start(): Unit = mongo.start()

  def stop(): Unit = mongo.stop()
}
