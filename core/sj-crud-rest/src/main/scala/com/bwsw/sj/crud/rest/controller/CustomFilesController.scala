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
package com.bwsw.sj.crud.rest.controller

import java.io.File

import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.CustomFilesSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.utils.FileMetadataUtils
import com.bwsw.sj.crud.rest.{CustomFile, CustomFilesResponseEntity}
import scaldi.Injectable.inject
import scaldi.Injector

class CustomFilesController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]
  private val fileMetadataUtils = inject[FileMetadataUtils]

  import messageResourceUtils._

  override val serviceInterface = inject[CustomFilesSI]

  protected val entityDeletedMessage: String = "rest.custom.files.file.deleted"
  protected val entityNotFoundMessage: String = "rest.custom.files.file.notfound"

  def create(entity: FileMetadataApi): RestResponse = {
    var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
      getMessage("rest.custom.files.file.missing")))

    if (entity.filename.isDefined) {
      val file = entity.customFileParts("file").asInstanceOf[File]
      entity.file = Some(file)

      entity.customFileParts.get("description").foreach { description =>
        entity.description = description.asInstanceOf[String]
      }

      val created = serviceInterface.create(entity.to())

      response = created match {
        case Created =>
          OkRestResponse(MessageResponseEntity(
            createMessage("rest.custom.files.file.uploaded", entity.filename.get)))
        case NotCreated(_) =>
          ConflictRestResponse(MessageResponseEntity(
            createMessage("rest.custom.files.file.exists", entity.filename.get)))
      }
    }

    response
  }

  override def getAll(): RestResponse = {
    val fileMetadata = serviceInterface.getAll().map(fileMetadataUtils.toCustomFileInfo)

    OkRestResponse(CustomFilesResponseEntity(fileMetadata))
  }

  override def get(name: String): RestResponse = {
    val fileMetadata = serviceInterface.get(name)

    fileMetadata match {
      case Some(x) =>
        val source = fileMetadataUtils.fileToSource(x.file.get)

        CustomFile(name, source)
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  override def create(serializedEntity: String): RestResponse = ???
}