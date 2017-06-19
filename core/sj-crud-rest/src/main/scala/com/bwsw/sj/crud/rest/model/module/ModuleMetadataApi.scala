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
package com.bwsw.sj.crud.rest.model.module

import java.io.File
import java.util.jar.JarFile

import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.common.utils.{MessageResourceUtils, RestLiterals}
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

class ModuleMetadataApi(filename: String,
                        file: File,
                        description: String = RestLiterals.defaultDescription,
                        customFileParts: Map[String, Any] = Map())
                       (implicit injector: Injector)
  extends FileMetadataApi(
    Option(filename),
    Option(file),
    description,
    customFileParts) {

  override def to(): ModuleMetadata =
    new ModuleMetadata(filename, inject[CreateSpecificationApi].from(file).to, Option(file))

  def validate: ArrayBuffer[String] = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

    val errors = new ArrayBuffer[String]

    if (!filename.endsWith(".jar"))
      errors += createMessage("rest.modules.modules.extension.unknown", filename)

    if (Try(new JarFile(file)).isFailure)
      errors += createMessage("rest.modules.module.jar.incorrect", filename)

    errors
  }
}
