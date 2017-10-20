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

import scala.util.{Failure, Success, Try}

/**
  * This is a service responsible for termination of run process (which have been started during test execution).
  * It is being called to terminate processes using java.lang.System.exit() method terminating the currently running
  * Java virtual machine.
  *
  * @author Pavel Tomskikh
  */
object ProcessTerminator {

  /**
    * Terminates the current process after executing the method
    *
    * @param f method after which the current process will be terminated
    */
  def terminateProcessAfter(f: () => Unit): Unit = {
    val exitCode = Try(f()) match {
      case Success(_) => 0
      case Failure(e) =>
        e.printStackTrace()
        1
    }

    System.exit(exitCode)
  }
}
