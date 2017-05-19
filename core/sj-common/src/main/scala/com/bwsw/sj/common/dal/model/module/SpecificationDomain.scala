package com.bwsw.sj.common.dal.model.module

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField

/**
  * Entity for specification-json of module
  *
  * @author Kseniya Tomskikh
  */
class SpecificationDomain(val name: String,
                          val description: String,
                          val version: String,
                          val author: String,
                          val license: String,
                          val inputs: IOstream,
                          val outputs: IOstream,
                          @PropertyField("module-type") val moduleType: String,
                          @PropertyField("engine-name") var engineName: String,
                          @PropertyField("engine-version") var engineVersion: String,
                          @PropertyField("validator-class") val validateClass: String,
                          @PropertyField("executor-class") val executorClass: String)
