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
package com.bwsw.sj.common.rest.utils

import org.apache.curator.utils.PathUtils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Provides helping methods for validation some fields of entities
  */
object ValidationUtils {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def validateName(name: String): Boolean = {
    logger.debug(s"Validate a name: '$name'.")
    name.matches( """^([a-z][a-z0-9-]*)$""")
  }

  def validateConfigSettingName(name: String): Boolean = {
    logger.debug(s"Validate a configuration name: '$name'.")
    name.matches( """^([a-z][a-z0-9-\.]*)$""")
  }

  def validateNamespace(namespace: String): Boolean = {
    logger.debug(s"Validate a namespace: '$namespace'.")
    namespace.matches( """^([a-z][a-z0-9_]*)$""")
  }

  def normalizeName(name: String): String = {
    logger.debug(s"Normalize a name: '$name'.")
    name.replace('\\', '/')
  }

  /**
    * Validates prefix in [[com.bwsw.sj.common.si.model.service.TStreamService TStreamService]]
    *
    * @return None if prefix is valid, Some(error) otherwise
    */
  def validatePrefix(prefix: String): Option[String] = {
    Try(PathUtils.validatePath(prefix)) match {
      case Success(_) => None
      case Failure(exception: Throwable) => Some(exception.getMessage)
    }
  }

  def validateToken(token: String): Boolean = {
    token.length <= 32
  }
}