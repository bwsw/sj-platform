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
package com.bwsw.sj.engine.regular.benchmark.utils

import java.io.{BufferedReader, File, FileReader}

/**
  * Provides some useful methods for benchmarks
  *
  * @author Pavel Tomskikh
  */
object BenchmarkUtils {

  /**
    * Retrieves result from file
    *
    * @param outputFile file that must contain result
    * @return result if a file exists or None otherwise
    */
  def retrieveResultFromFile(outputFile: File): Option[String] = {
    if (outputFile.exists()) {
      val reader = new BufferedReader(new FileReader(outputFile))
      val result = reader.readLine()
      reader.close()
      outputFile.delete()

      Some(result)
    }
    else
      None
  }
}
