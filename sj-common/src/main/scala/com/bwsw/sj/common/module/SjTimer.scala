package com.bwsw.sj.common.module

import java.util.{Timer, TimerTask}

/**
 * Class representing a timer
 * Created: 14/04/2016
 * @author Kseniya Mikhaleva
 */

class SjTimer {

  private var isTimerWentOut = false
  private var timer: Timer = null

  def setTimer(delay: Long) = {
    timer = new Timer()
    timer.schedule(new TimerTask {
      def run() {
        isTimerWentOut = true
      }
    }, delay)
  }

  def isTime: Boolean = {
    isTimerWentOut
  }

  def resetTimer() = {
    timer.cancel()
    isTimerWentOut = false
  }
}
