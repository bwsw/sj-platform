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
package com.bwsw.common.file.utils

import java.io.{File, FileInputStream, FileNotFoundException, InputStream}
import java.nio.file.FileAlreadyExistsException

import com.bwsw.sj.common.dal.model.module.SpecificationDomain
import org.apache.commons.io.FileUtils

/**
  * Provides methods to CRUD files using a specific local path
  *
  * @param pathToLocalStorage a specific local path where files will be stored
  */
class LocalStorage(pathToLocalStorage: String) extends FileStorage {
  override def put(file: File, fileName: String): Unit = {
    logger.debug(s"Try to put a file: '$fileName' in a local storage (path to local storage: $pathToLocalStorage).")
    val storageFile = new File(pathToLocalStorage + fileName)
    logger.debug(s"Check whether a local storage already contains a file with name: '$fileName' or not.")
    if (!storageFile.exists()) {
      logger.debug(s"Create file in a local storage. Absolute path of file: '${pathToLocalStorage + fileName}'.")
      FileUtils.copyFile(file, storageFile)
    } else {
      logger.error(s"File with name: '$fileName' already exists in a local storage.")
      throw new FileAlreadyExistsException(s"$fileName already exists")
    }
  }

  override def get(fileName: String, newFileName: String): File = {
    logger.debug(s"Try to get a file: '$fileName' from a local storage (path to local storage: $pathToLocalStorage).")
    val file: File = new File(newFileName)
    val storageFile = new File(pathToLocalStorage + fileName)
    logger.debug(s"Check whether a local storage contains a file with name: '$fileName' or not.")
    if (storageFile.exists()) {
      logger.debug(s"Copy a file: '$fileName' from a local storage to file with name: '$newFileName'.")
      FileUtils.copyFile(storageFile, file)

      file
    } else {
      logger.error(s"File with name: '$fileName' doesn't exist in a local storage.")
      throw new FileNotFoundException(s"$fileName doesn't exist")
    }
  }

  /**
    * Retrieve file as [[InputStream]]
    */
  override def getStream(fileName: String): InputStream = {
    logger.debug(s"Try to get a file: '$fileName' from a local storage (path to local storage: $pathToLocalStorage).")
    val storageFile = new File(pathToLocalStorage + fileName)
    logger.debug(s"Check whether a local storage contains a file with name: '$fileName' or not.")
    if (storageFile.exists()) {
      new FileInputStream(storageFile)
    } else {
      logger.error(s"File with name: '$fileName' doesn't exist in a local storage.")
      throw new FileNotFoundException(s"$fileName doesn't exist")
    }
  }

  override def delete(fileName: String): Boolean = {
    logger.debug(s"Try to delete a file: '$fileName' from a local storage (path to local storage: $pathToLocalStorage).")
    val file = new File(pathToLocalStorage + fileName)
    logger.debug(s"Check whether a local storage contains a file with name: '$fileName' or not.")
    if (file.exists) {
      logger.debug(s"Delete a file from a local storage. Absolute path of file: '${pathToLocalStorage + fileName}'.")
      file.delete()
    } else {
      logger.debug(s"Local storage doesn't contain a file with name: '$fileName'.")
      false
    }
  }

  override def put(file: File, fileName: String, specification: Map[String, Any], filetype: String): Unit = {
    throw new NotImplementedError("Local storage hasn't an opportunity to save file with specification yet.")
  }

  override def put(file: File, fileName: String, specification: SpecificationDomain, filetype: String): Unit = {
    throw new NotImplementedError("Local storage hasn't an opportunity to save file with specification yet.")
  }

  override def exists(fileName: String): Boolean = {
    logger.debug(s"Check whether a local storage contains a file with name: '$fileName' or not.")
    val file = new File(pathToLocalStorage + fileName)
    file.exists
  }
}
