package com.bwsw.sj.common.engine

import com.bwsw.sj.common.rest.entities.module.InstanceMetadata

/**
 * Trait for validating a launch parameters of a module of a specific type
 * (input, regular, output)
 *
 * @author Kseniya Mikhaleva
 */

trait StreamingValidator {
  /**
   * Provides a validation function that checks a propriety of option parameter of instance
   * (custom options)
   * @param options Option parameters
   * @return The result of the validation and a set of errors if it exists
   */
  def validate(options: Map[String, Any]): ValidationInfo = {
    ValidationInfo()
  }

  def validate(instance: InstanceMetadata): ValidationInfo = {
    ValidationInfo()
  }
}
