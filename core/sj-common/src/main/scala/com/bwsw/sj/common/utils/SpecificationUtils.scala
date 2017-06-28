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
package com.bwsw.sj.common.utils

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.module.SpecificationDomain

import scala.util.{Failure, Success, Try}

class SpecificationUtils {
  private val serializer = new JsonSerializer()
  private var maybeSpecification: Option[SpecificationDomain] = None

  /**
    * Retrieves [[SpecificationDomain]] from jar file
    *
    * @param jarFile
    * @return specification
    */
  def getSpecification(jarFile: File): SpecificationDomain = {
    maybeSpecification match {
      case Some(specification) =>

        specification
      case None =>
        val serializedSpecification = getSpecificationFromJar(jarFile)

        serializer.deserialize[SpecificationDomain](serializedSpecification)
    }
  }

  /**
    * Retrieves content of specification.json file from root of jar
    *
    * @param file jar file
    * @return content of specification.json
    */
  def getSpecificationFromJar(file: File): String = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        val result = Try {
          var line = reader.readLine
          while (Option(line).isDefined) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        }
        reader.close()
        result match {
          case Success(_) =>
          case Failure(e) => throw e
        }
      }
    }

    builder.toString()
  }
}
