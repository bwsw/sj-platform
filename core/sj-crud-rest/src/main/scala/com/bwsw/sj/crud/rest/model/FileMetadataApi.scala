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
package com.bwsw.sj.crud.rest.model

import java.io.File

import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.RestLiterals
import scaldi.Injector

class FileMetadataApi(var filename: Option[String] = None,
                      var file: Option[File] = None,
                      var description: String = RestLiterals.defaultDescription,
                      var customFileParts: Map[String, Any] = Map())
                     (implicit injector: Injector) {
  def to(): FileMetadata = {
    new FileMetadata(
      filename = filename.get,
      file = file,
      description = Some(description))
  }
}
