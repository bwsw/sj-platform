package com.bwsw.sj.common.engine

import com.bwsw.sj.common.rest.model.module.InstanceApi
import com.bwsw.sj.common.utils.EngineLiterals

/**
 * Trait for validating [[InstanceApi]] parameters of a module of a specific type [[EngineLiterals.moduleTypes]]
 *
 * @author Kseniya Mikhaleva
 */

trait StreamingValidator {
  /**
   * Provides a validation function that checks a propriety of [[InstanceApi.options]]

   * @return The result of the validation and a set of errors if they exist
   */
  def validate(options: String): ValidationInfo = {
    ValidationInfo()
  }

  def validate(instance: InstanceApi): ValidationInfo = {
    ValidationInfo()
  }
}
