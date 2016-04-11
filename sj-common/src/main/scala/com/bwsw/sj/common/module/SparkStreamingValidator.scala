package com.bwsw.sj.common.module

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.module.entities._

/**
 * Trait for validating a launch parameters of module
 * Created: 07/04/2016
 * @author Kseniya Mikhaleva
 */

trait SparkStreamingValidator {
  
  val serializer = new JsonSerializer()

  def validate(launchParameters: LaunchParameters): Boolean = {
     val specification = serializer.deserialize[ModuleSpecification](
       scala.io.Source.fromInputStream(getClass.getResourceAsStream("/specification.json")).mkString
     ) //use for checking
    if (launchParameters.instanceName.isEmpty || launchParameters.instanceType.moduleName.isEmpty) false
    
    validateOptions(launchParameters.options)
  }

  protected def validateOptions(options: Map[String, Any]): Boolean = {
    true
  }
}
