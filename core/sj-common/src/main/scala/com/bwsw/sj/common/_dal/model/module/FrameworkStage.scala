package com.bwsw.sj.common._dal.model.module

import java.util.Date

import com.bwsw.sj.common.utils.EngineLiterals._

/**
  * Stage of running instance
  *
  * @author Kseniya Tomskikh
  */
case class FrameworkStage(var state: String = toHandle, var datetime: Date = new Date(), var duration: Long = 0)
