package com.bwsw.sj.common.engine

import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals

/**
 * Trait for validating [[Instance]] parameters of a module of a specific type [[EngineLiterals.moduleTypes]]
 *
 * @author Kseniya Mikhaleva
 */

trait StreamingValidator {
  /**
   * Provides a validation function that checks a propriety of [[Instance.options]]

   * @return The result of the validation and a set of errors if they exist
   */
  def validate(options: String): ValidationInfo = {
    ValidationInfo()
  }

  def validate(instance: Instance): ValidationInfo = {
    ValidationInfo()
  }
}
