package com.bwsw.sj.engine.core.input

import com.bwsw.sj.engine.core.entities.InputEnvelope

/**
  * Class represents an interval of buffer, which contains incoming data of input module,
  * that defines the boundaries of [[InputEnvelope]]
  *
  * @param initialValue a number of byte that indicates the beginning of message
  * @param finalValue   a number of byte that indicates the end of message
  * @author Kseniya Mikhaleva
  */
case class Interval(initialValue: Int, finalValue: Int)
