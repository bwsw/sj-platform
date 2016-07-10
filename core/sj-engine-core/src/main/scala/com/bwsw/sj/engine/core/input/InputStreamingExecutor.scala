package com.bwsw.sj.engine.core.input

import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import io.netty.buffer.ByteBuf

/**
 * Class that contains an execution logic of input module
 * Created: 10/07/2016
 *
 * @author Kseniya Mikhaleva
 */

class InputStreamingExecutor(manager: InputEnvironmentManager) {
  /**
   * Will invoke every time when a new part of data is received
   * @param buffer Input stream is a flow of bytes
   * @return Interval into buffer that probably contains a message or None
   */
  def tokenize(buffer: ByteBuf): Option[(Int, Int)] = {
    None
  }

  /**
   * Will invoke after each calling tokenize method if tokenize doesn't return None
   * @param buffer Input stream is a flow of bytes
   * @param beginIndex Index of the beginning of a message
   * @param endIndex Index of the end of a message
   * @return Input envelope or None
   */
  def parse(buffer: ByteBuf, beginIndex: Int, endIndex: Int): Option[InputEnvelope] = {
    None
  }
}