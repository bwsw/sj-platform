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
package com.bwsw.sj.common.dal.model.module

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField

class BatchSpecificationDomain(name: String,
                               description: String,
                               version: String,
                               author: String,
                               license: String,
                               inputs: IOstream,
                               outputs: IOstream,
                               @PropertyField("module-type") moduleType: String,
                               @PropertyField("engine-name") engineName: String,
                               @PropertyField("engine-version") engineVersion: String,
                               @PropertyField("validator-class") validateClass: String,
                               @PropertyField("executor-class") executorClass: String,
                               @PropertyField("batch-collector-class") val batchCollectorClass: String)
  extends SpecificationDomain(
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
    validateClass,
    executorClass) {

}
