package com.bwsw.sj.common.si.model.module

import com.bwsw.sj.common.dal.model.module.{IOstream, SpecificationDomain}

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
                    val options: Map[String, Any],
                    val validatorClass: String,
                    val executorClass: String) {

  def to: SpecificationDomain = ???

  def validate: ArrayBuffer[String] = validateGeneralFields

  protected def validateGeneralFields: ArrayBuffer[String] = ???

}

