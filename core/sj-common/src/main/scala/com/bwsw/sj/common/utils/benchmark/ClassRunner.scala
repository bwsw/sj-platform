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
package com.bwsw.sj.common.utils.benchmark

import scala.collection.JavaConverters._

/**
  * Launches an application in a separate process. Class must contain static method main() (or main() in companion object).
  *
  * @param clazz       class name of application to launch, must contain a main method
  * @param environment environment variables
  * @param arguments   command line arguments
  * @param properties  system properties
  * @author Pavel Tomskikh
  */
class ClassRunner(clazz: Class[_],
                  environment: Map[String, Any] = Map.empty,
                  arguments: Seq[String] = Seq.empty,
                  properties: Map[String, Any] = Map.empty) {
  private val javaLauncher = System.getProperty("java.home") + "/bin/java"
  private val transformedProperties = properties.map { case (property, value) => s"-D$property=$value" }
  private val classPath = Seq("-classpath", System.getProperty("java.class.path"))
  private val command = Seq(javaLauncher) ++ transformedProperties ++ classPath ++ Seq(clazz.getName) ++ arguments

  private val processBuilder = new ProcessBuilder(command.asJava).inheritIO()
  processBuilder.environment().putAll(environment.mapValues(_.toString).asJava)

  def start(): Process = processBuilder.start()
}
