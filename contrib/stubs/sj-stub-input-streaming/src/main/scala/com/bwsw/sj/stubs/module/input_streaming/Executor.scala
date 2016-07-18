package com.bwsw.sj.stubs.module.input_streaming

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.{Interval, InputStreamingExecutor}
import io.netty.buffer.ByteBuf


class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
  val outputs = manager.getStreamsByTags(Array("output"))

  /**
   * Will be invoked every time when a new part of data is received
   * @param buffer Input stream is a flow of bytes
   * @return Interval into buffer that probably contains a message or None
   */
  override def tokenize(buffer: ByteBuf): Option[Interval] = {
    Some(Interval(0, buffer.readableBytes()))
  }

  /**
   * Will be invoked after each calling tokenize method if tokenize doesn't return None
   * @param buffer Input stream is a flow of bytes
   * @param interval Defines the boundaries of an input envelope
   * @return Input envelope or None
   */
  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope] = {
    val envelope = new InputEnvelope("key", Array((outputs.head, 0)), true,
      buffer.slice(interval.initialValue, interval.finalValue).array())

    Some(envelope)
  }
}
