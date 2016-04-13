package com.bwsw.sj.common.module.entities

/**
 * Class representing a structure of module specification json
 * Created: 11/04/2016
 * @author Kseniya Mikhaleva
 */

case class ModuleSpecification(name: String,
                               description: String,
                               version: String,
                               author: String,
                               license: String,
                               inputs: SourceMetadata,
                               outputs: SourceMetadata,
                               `module-type`: String,
                               engine: String,
                               options: Map[String, Any],
                               `validator-class`: String,
                               `executor-class`: String
                                )
