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
package com.bwsw.sj.engine.core.testutils.benchmark.sj

import java.io.File

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, InstanceDomain}
import com.bwsw.sj.common.dal.model.module.SpecificationDomain
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.{Injector, Module}

/**
  * Provides method to create instance and module's specification
  *
  * @author Pavel Tomskikh
  */
trait InstanceFactory[T <: InstanceDomain] {

  implicit protected val injector: Injector = new Module {
    bind[MessageResourceUtils] to new MessageResourceUtils
    bind[JsonSerializer] toProvider new JsonSerializer(ignoreUnknown = true, enableNullForPrimitives = true)
  }.injector

  /**
    * Creates instance and module's specification
    *
    * @return (instance, specification)
    */
  def create(module: File,
             name: String,
             coordinationService: ZKServiceDomain,
             inputStream: StreamDomain,
             outputStream: StreamDomain,
             executionPlan: ExecutionPlan): (T, SpecificationDomain)
}
