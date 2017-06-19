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
package com.bwsw.sj.crud.rest.utils

import java.io.File
import java.nio.file.Paths

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.crud.rest.{CustomFileInfo, CustomJarInfo, ModuleInfo}

import scala.concurrent.Future

class FileMetadataUtils {
  def toCustomJarInfo(fileMetadata: FileMetadata): CustomJarInfo = {
    CustomJarInfo(
      fileMetadata.name.get,
      fileMetadata.version.get,
      fileMetadata.length.get)
  }

  def toCustomFileInfo(fileMetadata: FileMetadata): CustomFileInfo = {
    CustomFileInfo(
      fileMetadata.filename,
      fileMetadata.description.get,
      fileMetadata.uploadDate.get,
      fileMetadata.length.get)
  }

  def fileToSource(file: File): Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(Paths.get(file.getAbsolutePath))

  def toModuleInfo(moduleMetadata: ModuleMetadata): ModuleInfo = {
    ModuleInfo(
      moduleMetadata.specification.moduleType,
      moduleMetadata.specification.name,
      moduleMetadata.specification.version,
      moduleMetadata.length.get)
  }
}
