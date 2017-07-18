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
package com.bwsw.common.file.utils

import java.io.{File, InputStream}

import com.bwsw.sj.common.dal.model.module.SpecificationDomain
import org.slf4j.{Logger, LoggerFactory}

/**
  * Provides methods to CRUD files using a specific storage
  */
trait FileStorage {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def put(file: File, fileName: String): Unit

  def put(file: File, fileName: String, specification: Map[String, Any], filetype: String): Unit

  def put(file: File, fileName: String, specification: SpecificationDomain, filetype: String): Unit

  def get(fileName: String, newFileName: String): File

  /**
    * Retrieve file as [[java.io.InputStream]]
    */
  def getStream(fileName: String): InputStream

  def delete(fileName: String): Boolean

  def exists(fileName: String): Boolean

}
