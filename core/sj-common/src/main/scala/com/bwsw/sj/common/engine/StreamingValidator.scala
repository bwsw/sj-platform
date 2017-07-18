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
package com.bwsw.sj.common.engine

import com.bwsw.sj.common.si.model.instance.Instance

/**
  * Trait for validating [[com.bwsw.sj.common.si.model.instance.Instance]] parameters of a module of a specific type
  * [[com.bwsw.sj.common.utils.EngineLiterals.moduleTypes]]
  *
  * @author Kseniya Mikhaleva
  */

trait StreamingValidator {
  /**
    * Provides a validation function that checks a propriety of [[com.bwsw.sj.common.si.model.instance.Instance.options]]
    *
    * @return The result of the validation and a set of errors if they exist
    */
  def validate(options: String): ValidationInfo = {
    ValidationInfo()
  }

  def validate(instance: Instance): ValidationInfo = {
    ValidationInfo()
  }
}
