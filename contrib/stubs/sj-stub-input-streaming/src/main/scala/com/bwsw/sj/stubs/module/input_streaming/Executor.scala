package com.bwsw.sj.stubs.module.input_streaming

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import io.netty.buffer.ByteBuf


class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
  val outputs = manager.getStreamsByTags(Array("output"))

  /**
   * Will invoke every time when a new part of data is received
   * @param buffer Input stream is a flow of bytes
   * @return Interval into buffer that probably contains a message or None
   */
  override def tokenize(buffer: ByteBuf): Option[(Int, Int)] = {
    Some(0, buffer.capacity() - 1)
  }

  /**
   * Will invoke after each calling tokenize method if tokenize doesn't return None
   * @param buffer Input stream is a flow of bytes
   * @param beginIndex Index of the beginning of a message
   * @param endIndex Index of the end of a message
   * @return Input envelope or None
   */
  override def parse(buffer: ByteBuf, beginIndex: Int, endIndex: Int): Option[InputEnvelope] = {
    val envelope = new InputEnvelope("key", Array((outputs.head, 0)), true, buffer.slice(beginIndex, endIndex).array())

    Some(envelope)
  }
}
