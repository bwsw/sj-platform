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

import com.bwsw.sj.common.engine.StreamingValidator

/**
  * Provides useful methods for [[StreamingValidator]] implementation
  *
  * @author Pavel Tomskikh
  */
object ValidationUtils {

  def isOptionStringField(string: Option[String]): Boolean = string.isEmpty || string.get.nonEmpty

  def isRequiredStringField(string: Option[String]): Boolean = string.nonEmpty && string.get.nonEmpty

  def checkFields(fields: Option[List[String]], uniqueKey: Option[List[String]], distribution: Option[List[String]]): Boolean = {
    fields match {
      case Some(fieldNames: List[String]) =>
        fieldNames.nonEmpty &&
          fieldNames.forall(x => isRequiredStringField(Option(x))) &&
          (uniqueKey.isEmpty || uniqueKey.get.forall(fieldNames.contains)) &&
          (distribution.isEmpty || distribution.get.forall(fieldNames.contains))
      case _ => false
    }
  }
}