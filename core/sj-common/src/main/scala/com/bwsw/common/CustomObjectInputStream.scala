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
package com.bwsw.common

import java.io.{InputStream, ObjectInputStream, ObjectStreamClass}

import scala.util.{Failure, Success, Try}

/**
  * Wrapper for [[ObjectInputStream]] with additional [[ClassLoader]] to load user defined classes from module
  *
  * @param classLoader extended class loader (with classes from module)
  * @param in          an input stream of bytes
  */
class CustomObjectInputStream(private var classLoader: ClassLoader,
                              in: InputStream)
  extends ObjectInputStream(in) {

  protected override def resolveClass(desc: ObjectStreamClass): Class[_] =
    Try {
      val name: String = desc.getName
      Class.forName(name, false, classLoader)
    } match {
      case Success(clazz) => clazz
      case Failure(_: ClassNotFoundException) => super.resolveClass(desc)
      case Failure(e) => throw e
    }
}
