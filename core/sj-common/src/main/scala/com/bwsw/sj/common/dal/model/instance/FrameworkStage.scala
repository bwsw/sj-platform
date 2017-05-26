package com.bwsw.sj.common.dal.model.instance

import java.util.Date

import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.EngineLiterals

/**
  * Information about an instance that has been launched
  *
  * @param state    state of instance one of the following [[EngineLiterals.instanceStatusModes]]
  * @param datetime last time when a state has been changed
  * @param duration how long an instance has got current state
  * @author Kseniya Tomskikh
  */
//todo описать связь фреймворка и инстанса, точно пригодится в документации
case class FrameworkStage(var state: String = toHandle, var datetime: Date = new Date(), var duration: Long = 0)
