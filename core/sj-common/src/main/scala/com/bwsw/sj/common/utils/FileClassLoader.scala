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
package com.bwsw.sj.common.utils

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import scaldi.Injectable.inject
import scaldi.Injector

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Provides method for loading class from file in [[com.bwsw.common.file.utils.FileStorage FileStorage]]
  */
class FileClassLoader {
  /**
    * directory for temporary file saving
    */
  private val tmpDirectory = "/tmp/"

  /**
    * Loads the class from file that is in [[com.bwsw.common.file.utils.FileStorage FileStorage]]
    * and returns an instance of this class
    *
    * @param className    name of class
    * @param filename     name of file
    */
  def getInstance(className: String, filename: String)
                 (implicit injector: Injector): Any = {
    val storage = inject[ConnectionRepository].getFileStorage
    val file = storage.get(filename, tmpDirectory + filename)
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val instance = loader.loadClass(className).newInstance()
    file.delete()

    instance
  }
}
