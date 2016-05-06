package com.bwsw.sj.common.module.environment

import scala.collection.mutable

/**
 * Provides an output stream that defined for stream in whole.
 * Recording of transaction occurs with the use of round-robin policy
 * Created: 20/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param output Output of stream
 */

class RoundRobinOutput(output: mutable.MutableList[Array[Byte]]) {
  def put(data: Array[Byte]) = output.+=(data)
}
