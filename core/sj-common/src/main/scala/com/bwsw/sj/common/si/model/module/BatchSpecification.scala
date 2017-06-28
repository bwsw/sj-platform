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
package com.bwsw.sj.common.si.model.module

import com.bwsw.sj.common.dal.model.module.{BatchSpecificationDomain, IOstream}
import com.bwsw.sj.common.utils.EngineLiterals
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class BatchSpecification(name: String,
                         description: String,
                         version: String,
                         author: String,
                         license: String,
                         inputs: IOstream,
                         outputs: IOstream,
                         engineName: String,
                         engineVersion: String,
                         validatorClass: String,
                         executorClass: String,
                         val batchCollectorClass: String)
                        (implicit injector: Injector)
  extends Specification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    EngineLiterals.batchStreamingType,
    engineName,
    engineVersion,
    validatorClass,
    executorClass) {

  import messageResourceUtils.createMessage

  override def to: BatchSpecificationDomain = {
    new BatchSpecificationDomain(
      name,
      description,
      version,
      author,
      license,
      inputs,
      outputs,
      moduleType,
      engineName,
      engineVersion,
      validatorClass,
      executorClass,
      batchCollectorClass)
  }

  override def validate: ArrayBuffer[String] = {
    val errors = validateGeneralFields

    Option(batchCollectorClass) match {
      case None | Some("") =>
        errors += createMessage("rest.validator.specification.attribute.required", "batch-collector-class")
      case _ =>
    }

    errors
  }
}


