package com.bwsw.sj.common.module.state

/**
 * Trait representing interface for interaction with a state of module
 * Created: 19/04/2016
 * @author Kseniya Mikhaleva
 */

trait State {

  def get(key: String): Any

  def set(key: String, value: Any)

  def delete(key: String)
}
