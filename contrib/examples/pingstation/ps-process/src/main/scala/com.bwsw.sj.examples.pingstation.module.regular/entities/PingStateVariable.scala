package com.bwsw.sj.examples.pingstation.module.regular.entities

/**
 * Class is in charge of collecting statistics of fping command
 * @param ts Last timestamp of the collected responses
 * @param totalTime Sum of average time of fping response
 * @param totalOk Total amount of successful responses
 * @param totalUnreachable Total amount of unsuccessful responses
 */

case class PingStateVariable(var ts: Long, var totalTime: Double, var totalOk: Long, var totalUnreachable: Long)
