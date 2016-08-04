package com.bwsw.sj.common.engine

/**
 * Trait for validating a launch parameters of module
 * Created: 07/04/2016
 * @author Kseniya Mikhaleva
 */

trait StreamingValidator {
  /**
   * Provides a validation function that checks a propriety of option parameters
   * @param options Option parameters
   * @return The result of the validation. True by default
   */
  def validate(options: Map[String, Any]): Boolean = {
    true
  }
}
