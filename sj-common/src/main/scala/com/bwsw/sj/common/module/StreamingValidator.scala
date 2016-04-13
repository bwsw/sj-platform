package com.bwsw.sj.common.module

/**
 * Trait for validating a launch parameters of module
 * Created: 07/04/2016
 * @author Kseniya Mikhaleva
 */

trait StreamingValidator {

  def validate(options: Map[String, Any]): Boolean = {
    true
  }
}
