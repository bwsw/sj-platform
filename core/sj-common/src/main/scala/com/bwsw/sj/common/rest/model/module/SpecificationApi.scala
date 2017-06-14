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
package com.bwsw.sj.common.rest.model.module

/**
  * API entity for instance specification
  */
case class SpecificationApi(var name: String,
                            var description: String,
                            var version: String,
                            var author: String,
                            var license: String,
                            inputs: Map[String, Any],
                            outputs: Map[String, Any],
                            var moduleType: String,
                            var engineName: String,
                            var engineVersion: String,
                            options: Map[String, Any],
                            var validateClass: String,
                            executorClass: String)
