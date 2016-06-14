package com.bwsw.sj.common.utils

import java.util.{Timer, TimerTask}

import org.slf4j.LoggerFactory

/**
 * Class representing a wrapper for java.util.Timer
 * Created: 14/04/2016
 * @author Kseniya Mikhaleva
 */

class SjTimer {

  private val logger = LoggerFactory.getLogger(this.getClass)
  /**
   * Flag defines the timer went out or not
   */
  private var isTimerWentOut = false

  private var timer: Timer = null

  /**
   * Time when timer went out. Needed for computing lag between a real response time
   * and an invoke of time handler
   */
  var responseTime = 0L

  /**
   * Sets a timer handler that changes flag on true value when time is went out
   * @param delay delay in milliseconds before timer task is to be executed
   */
  def set(delay: Long) = {
    logger.info(s"Set a timer to $delay\n")
    timer = new Timer()
    timer.schedule(new TimerTask {
      def run() {
        isTimerWentOut = true
        responseTime = System.currentTimeMillis()
      }
    }, delay)
  }

  /**
   * Allows checking a timer has went out or not
   * @return The result of checking
   */
  def isTime: Boolean = {
    logger.debug(s"Check whether a timer has went out or not\n")
    isTimerWentOut
  }

  /**
   * Allows resetting a timer 
   */
  def reset() = {
    logger.debug(s"Reset a timer\n")
    timer.cancel()
    isTimerWentOut = false
    responseTime = 0
  }
}
