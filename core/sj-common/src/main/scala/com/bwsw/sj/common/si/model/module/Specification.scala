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
package com.bwsw.sj.common.si.model.module

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.module.{BatchSpecificationDomain, IOstream, SpecificationDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{EngineLiterals, MessageResourceUtils, StreamLiterals}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class Specification(val name: String,
                    val description: String,
                    val version: String,
                    val author: String,
                    val license: String,
                    val inputs: IOstream,
                    val outputs: IOstream,
                    val moduleType: String,
                    val engineName: String,
                    val engineVersion: String,
                    val validatorClass: String,
                    val executorClass: String)
                   (implicit injector: Injector) {

  protected val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  def to: SpecificationDomain = {
    new SpecificationDomain(
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

  def validate: ArrayBuffer[String] = validateGeneralFields

  protected def validateGeneralFields: ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    Option(name) match {
      case None | Some("") =>
        errors += createMessage("rest.validator.specification.attribute.required", "name")
      case _ =>
    }

    var moduleTypeIsCorrect: Boolean = false

    Option(moduleType) match {
      case None | Some("") =>
        errors += createMessage("rest.validator.specification.attribute.required", "module-type")
      case Some(x) if !EngineLiterals.moduleTypes.contains(x) =>
        errors += createMessage(
          "rest.validator.specification.attribute.must.one.of",
          "module-type",
          EngineLiterals.moduleTypes.mkString("[", ", ", "]"))
      case _ =>
        moduleTypeIsCorrect = true
    }

    var inputsDefined: Boolean = false

    Option(inputs) match {
      case None =>
        errors += createMessage("rest.validator.specification.attribute.required", "inputs")
      case _ =>
        inputsDefined = true
    }

    Option(outputs) match {
      case None =>
        errors += createMessage("rest.validator.specification.attribute.required", "outputs")
      case _ if moduleTypeIsCorrect && inputsDefined =>
        errors ++= validateSources
      case _ =>
    }

    Option(engineName) match {
      case None | Some("") =>
        errors += createMessage("rest.validator.specification.attribute.required", "engine-name")
      case _ => Option(engineVersion) match {
        case None | Some("") =>
          errors += createMessage("rest.validator.specification.attribute.required", "engine-version")
        case _ =>
          val engine = engineName + "-" + engineVersion
          val configService = inject[ConnectionRepository].getConfigRepository
          if (configService.get(ConfigLiterals.systemDomain + "." + engine).isEmpty)
            errors += createMessage("rest.validator.specification.invalid.engine.params")
      }
    }

    Option(executorClass) match {
      case None | Some("") =>
        errors += createMessage("rest.validator.specification.attribute.required", "executor-class")
      case _ =>
    }

    Option(validatorClass) match {
      case None | Some("") =>
        errors += createMessage("rest.validator.specification.attribute.required", "validator-class")
      case _ =>
    }

    errors
  }

  private def validateSources: ArrayBuffer[String] = {
    val errors = validateSourceDefined(inputs, "inputs") ++ validateSourceDefined(outputs, "outputs")

    if (errors.isEmpty) {
      moduleType match {
        case EngineLiterals.inputStreamingType =>
          if (inputs.cardinality.exists(_ != 0))
            errors += createMessage("rest.validator.specification.both.cardinality", "inputs", "0")
          if (inputs.types.length != 1 || !inputs.types.contains(StreamLiterals.inputDummy))
            errors += createMessage("rest.validator.specification.input.type", StreamLiterals.inputDummy)
          if (outputs.types.isEmpty || !StreamLiterals.internalTypes.containsSlice(outputs.types))
            errors += createMessage("rest.validator.specification.sources.t-stream.kafka", "outputs")
          if (outputs.cardinality(0) <= 0 || outputs.cardinality(1) < outputs.cardinality(0))
            errors += createMessage("rest.validator.specification.cardinality.left.bound.greater.zero", "outputs")

        case EngineLiterals.outputStreamingType =>
          if (inputs.cardinality.exists(_ != 1))
            errors += createMessage("rest.validator.specification.both.cardinality", "inputs", "1")
          if (inputs.types.isEmpty || !StreamLiterals.internalTypes.containsSlice(inputs.types))
            errors += createMessage("rest.validator.specification.sources.t-stream.kafka", "inputs")
          if (outputs.cardinality.exists(_ != 1))
            errors += createMessage("rest.validator.specification.both.cardinality", "outputs", "1")
          if (outputs.types.isEmpty || !StreamLiterals.outputTypes.containsSlice(outputs.types))
            errors += createMessage("rest.validator.specification.sources.es.jdbc.rest", "outputs")

        case _ =>
          if (inputs.cardinality(0) <= 0 || inputs.cardinality(1) < inputs.cardinality(0))
            errors += createMessage("rest.validator.specification.cardinality.left.bound.greater.zero", "inputs")
          if (inputs.types.isEmpty || !StreamLiterals.internalTypes.containsSlice(inputs.types))
            errors += createMessage("rest.validator.specification.sources.t-stream.kafka", "inputs")
          if (outputs.types.isEmpty || !StreamLiterals.internalTypes.containsSlice(outputs.types))
            errors += createMessage("rest.validator.specification.sources.t-stream.kafka", "outputs")
          if (outputs.cardinality(0) <= 0 || outputs.cardinality(1) < outputs.cardinality(0))
            errors += createMessage("rest.validator.specification.cardinality.left.bound.greater.zero", "outputs")
      }
    }

    errors
  }

  private def validateSourceDefined(source: IOstream, attribute: String): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    Option(source.cardinality) match {
      case None =>
        errors += createMessage("rest.validator.specification.attribute.required", s"$attribute.cardinality")
      case Some(x) if x.length != 2 =>
        errors += createMessage("rest.validator.specification.cardinality", attribute)
      case _ =>
    }

    Option(source.types) match {
      case None =>
        errors += createMessage("rest.validator.specification.attribute.required", s"$attribute.types")
      case Some(x) if x.isEmpty =>
        errors += createMessage("rest.validator.specification.attribute.required", s"$attribute.types")
      case _ =>
    }

    errors
  }
}

object Specification {
  def from(specificationDomain: SpecificationDomain)(implicit injector: Injector): Specification = {
    specificationDomain.moduleType match {
      case EngineLiterals.batchStreamingType =>
        val batchSpecificationDomain = specificationDomain.asInstanceOf[BatchSpecificationDomain]

        new BatchSpecification(
          batchSpecificationDomain.name,
          batchSpecificationDomain.description,
          batchSpecificationDomain.version,
          batchSpecificationDomain.author,
          batchSpecificationDomain.license,
          batchSpecificationDomain.inputs,
          batchSpecificationDomain.outputs,
          batchSpecificationDomain.engineName,
          batchSpecificationDomain.engineVersion,
          batchSpecificationDomain.validateClass,
          batchSpecificationDomain.executorClass,
          batchSpecificationDomain.batchCollectorClass)

      case _ =>
        new Specification(
          specificationDomain.name,
          specificationDomain.description,
          specificationDomain.version,
          specificationDomain.author,
          specificationDomain.license,
          specificationDomain.inputs,
          specificationDomain.outputs,
          specificationDomain.moduleType,
          specificationDomain.engineName,
          specificationDomain.engineVersion,
          specificationDomain.validateClass,
          specificationDomain.executorClass)
    }
  }
}
