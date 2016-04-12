package com.bwsw.sj.common.module

import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.module.entities._

/**
 * Trait for validating a launch parameters of module
 * Created: 07/04/2016
 * @author Kseniya Mikhaleva
 */

trait StreamingValidator {
  
  val serializer: Serializer

  def validate(launchParameters: LaunchParameters): Boolean = {
     val specification = serializer.deserialize[ModuleSpecification](
       scala.io.Source.fromInputStream(getClass.getResourceAsStream("/specification.json")).mkString
     ) //use for checking
    if (launchParameters.instanceName.isEmpty) false
    
    validateOptions(launchParameters.options)
  }

  protected def validateOptions(options: Map[String, Any]): Boolean = {
    true
  }
}
