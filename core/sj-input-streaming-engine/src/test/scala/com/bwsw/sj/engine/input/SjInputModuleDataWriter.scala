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
package com.bwsw.sj.engine.input

import java.io.PrintStream
import java.net.Socket
import java.util.logging.LogManager

import com.bwsw.sj.engine.input.DataFactory.instancePort
import com.bwsw.sj.engine.input.SjInputModuleBenchmarkConstants.{instanceHost, numberOfDuplicates, totalInputElements}

import scala.util.{Failure, Success, Try}

/**
  * @author Pavel Tomskikh
  */
object SjInputModuleDataWriter extends App {
  LogManager.getLogManager.reset()
  val exitCode = writeData(totalInputElements, numberOfDuplicates)
  System.exit(exitCode)

  private def writeData(totalInputElements: Int, numberOfDuplicates: Int): Int = {
    Try {
      val socket = new Socket(instanceHost, instancePort)
      var amountOfDuplicates = -1
      var amountOfElements = 0
      var currentElement = 1
      val out = new PrintStream(socket.getOutputStream)

      while (amountOfElements < totalInputElements) {
        if (amountOfDuplicates != numberOfDuplicates) {
          out.println(currentElement)
          out.flush()
          amountOfElements += 1
          amountOfDuplicates += 1
        }
        else {
          currentElement += 1
          out.println(currentElement)
          out.flush()
          amountOfElements += 1
        }
      }

      socket.close()
    } match {
      case Success(_) =>
        println("DONE")
        0
      case Failure(e) =>
        println("init error: " + e)
        1
    }
  }
}

class SjInputModuleDataWriter
