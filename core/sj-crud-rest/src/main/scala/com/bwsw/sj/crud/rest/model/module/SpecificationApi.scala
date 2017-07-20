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

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.si.model.module._
import com.bwsw.sj.common.utils.{EngineLiterals, MessageResourceUtils}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.util.Try

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "module-type", defaultImpl = classOf[SpecificationApi], visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(Array(
  new Type(value = classOf[BatchSpecificationApi], name = EngineLiterals.batchStreamingType)))
class SpecificationApi(val name: String,
                       val description: String,
                       val version: String,
                       val author: String,
                       val license: String,
                       val inputs: IOstream,
                       val outputs: IOstream,
                       @JsonProperty("module-type") val moduleType: String,
                       @JsonProperty("engine-name") val engineName: String,
                       @JsonProperty("engine-version") val engineVersion: String,
                       @JsonProperty("validator-class") val validatorClass: String,
                       @JsonProperty("executor-class") val executorClass: String) {

  @JsonIgnore
  def to(implicit injector: Injector): Specification = new Specification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    moduleType,
    engineName,
    engineVersion,
    validatorClass,
    executorClass)
}

class SpecificationApiCreator(implicit injector: Injector) {
  private val serializer = inject[JsonSerializer]

  def from(specification: Specification): SpecificationApi = {
    specification.moduleType match {
      case EngineLiterals.batchStreamingType =>
        val batchSpecification = specification.asInstanceOf[BatchSpecification]
        new BatchSpecificationApi(
          batchSpecification.name,
          batchSpecification.description,
          batchSpecification.version,
          batchSpecification.author,
          batchSpecification.license,
          batchSpecification.inputs,
          batchSpecification.outputs,
          batchSpecification.engineName,
          batchSpecification.engineVersion,
          batchSpecification.validatorClass,
          batchSpecification.executorClass,
          batchSpecification.batchCollectorClass)

      case _ =>
        new SpecificationApi(
          specification.name,
          specification.description,
          specification.version,
          specification.author,
          specification.license,
          specification.inputs,
          specification.outputs,
          specification.moduleType,
          specification.engineName,
          specification.engineVersion,
          specification.validatorClass,
          specification.executorClass)
    }
  }

  def from(file: File): SpecificationApi = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.getMessage

    val jar = new JarFile(file)
    val enu = jar.entries().asScala
    enu.find(_.getName == "specification.json") match {
      case Some(entry) =>
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        val result = Try {
          val json = reader.lines().toArray.mkString("")
          serializer.deserialize[SpecificationApi](json)
        }
        reader.close()
        result.get
      case None =>
        throw new NoSuchElementException(getMessage("rest.modules.specification.json.not.found"))
    }
  }
}
