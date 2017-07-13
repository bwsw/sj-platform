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

import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net, RuntimeConfigBuilder}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{Command, MongodStarter}
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.io.{Processors, Slf4jLevel}
import org.slf4j.LoggerFactory

/**
  * Used for testing purposes only. Mongo DB server.
  *
  * @param port           mongo port
  * @param logIntoConsole flag denotes that logs will be writen into the console (true) or into a log-file (false)
  * @author Pavel Tomskikh
  */
class EmbeddedMongo(port: Int, logIntoConsole: Boolean = false) {
  private val config = new MongodConfigBuilder().net(new Net(port, false)).version(Version.V3_5_1).build()
  private val mongodStarter = createMongodStarter()
  private val mongo = mongodStarter.prepare(config)

  def start(): Unit = mongo.start()

  def stop(): Unit = mongo.stop()


  private def createMongodStarter(): MongodStarter = {
    if (logIntoConsole)
      MongodStarter.getDefaultInstance
    else {
      val logger = LoggerFactory.getLogger(getClass)
      val processOutput = new ProcessOutput(
        Processors.logTo(logger, Slf4jLevel.INFO),
        Processors.logTo(logger, Slf4jLevel.INFO),
        Processors.logTo(logger, Slf4jLevel.INFO))
      val runtimeConfig = new RuntimeConfigBuilder().defaults(Command.MongoD).processOutput(processOutput).build()

      MongodStarter.getInstance(runtimeConfig)
    }
  }
}
