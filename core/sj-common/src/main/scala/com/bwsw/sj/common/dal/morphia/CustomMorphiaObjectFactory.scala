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
package com.bwsw.sj.common.dal.morphia

import java.lang.reflect.Constructor

import com.bwsw.common.JsonSerializer
import org.mongodb.morphia.mapping.{DefaultCreator, MappingException}
import sun.reflect.ReflectionFactory

import scala.util.{Failure, Success, Try}

/**
  * Implementation of [[org.mongodb.morphia.ObjectFactory]] to avoid the redundant default constructor in entities.
  * {{ref. https://blog.jayway.com/2012/02/28/configure-morphia-to-work-without-a-default-constructor/}}
  */

class CustomMorphiaObjectFactory extends DefaultCreator {

  override def createInstance[T](clazz: Class[T]): T = Try(getNoArgsConstructor(clazz)) match {
    case Success(constructor) =>
      if (constructor.isDefined) {
        return clazz.cast(constructor.get.newInstance())
      }
      Try {
        val instance = ReflectionFactory.getReflectionFactory
          .newConstructorForSerialization(clazz, classOf[AnyRef].getDeclaredConstructor())
          .newInstance()

        clazz.cast(instance)
      } match {
        case Success(instance) => instance
        case Failure(e) => throw new MappingException("Failed to instantiate " + clazz.getName, e);
      }
    case Failure(e) => throw new RuntimeException(e);
  }

  private def getNoArgsConstructor(constructorType: Class[_]): Option[Constructor[_]] =
    Try {
      val constructor = constructorType.getDeclaredConstructor()
      constructor.setAccessible(true)

      constructor
    } match {
      case Success(constructor) => Some(constructor)
      case Failure(_: NoSuchMethodException) => None
      case Failure(e) => throw e
    }

  /**
    * It's necessary because of when a MesosSchedulerDriver (in mesos framework) is being created something is going wrong
    * (probably it should be but it's not our case) and after it the all instances have a null value of class loader.
    * May be it is a temporary measure (if we found a different solution)
    */
  override def getClassLoaderForClass: ClassLoader = {
    classOf[JsonSerializer].getClassLoader
  }
}
