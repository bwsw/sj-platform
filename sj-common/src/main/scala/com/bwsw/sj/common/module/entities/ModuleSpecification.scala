package com.bwsw.sj.common.module.entities

/**
 * Class represents a structure of module specification json
 * Created: 11/04/2016
 * @author Kseniya Mikhaleva
 */

case class ModuleSpecification(name: String,
                               description: String,
                               version: String,
                               author: String,
                               license: String,
                               inputs: Source,
                               outputs: Source,
                               `module-type`: String,
                               engine: String,
                               options: Map[String, Any],
                               `validator-class`: String,
                               `executor-class`: String
                                )
