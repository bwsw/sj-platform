package com.bwsw.sj.common.module.state

/**
 * Сlass representing state of module.
 * It may be used to keeping some global module variables related to processing
 * Created: 19/04/2016
 * @author Kseniya Mikhaleva
 *
 */

class StateStorage(stateService: IStateService) {
  
  def get(key: String): Any = {
    stateService.get(key)
  }

  def set(key: String, value: Any): Unit = {
    stateService.setChange(key, value)
    stateService.set(key,value)
  }

  def delete(key: String): Unit = {
    stateService.deleteChange(key)
    stateService.delete(key)
  }

}
