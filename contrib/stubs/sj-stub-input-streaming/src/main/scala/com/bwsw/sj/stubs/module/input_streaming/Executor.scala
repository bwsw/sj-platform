package com.bwsw.sj.stubs.module.input_streaming

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.{InputStreamingExecutor, Interval}
import io.netty.buffer.ByteBuf


class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor(manager) {

  var key = 1
  val objectSerializer = new ObjectSerializer()
  val outputs = manager.getStreamsByTags(Array("output"))

  /**
   * Will be invoked every time when a new part of data is received
   * @param buffer Input stream is a flow of bytes
   * @return Interval into buffer that probably contains a message or None
   */
  override def tokenize(buffer: ByteBuf): Option[Interval] = {
    val writeIndex = buffer.writerIndex()
    val endIndex = buffer.indexOf(0, writeIndex, 10)

    if (endIndex != -1) Some(Interval(0, endIndex)) else None
  }

  /**
   * Will be invoked after each calling tokenize method if tokenize doesn't return None
   * @param buffer Input stream is a flow of bytes
   * @return Input envelope or None
   */
  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope] = {

    val rawData = buffer.slice(interval.initialValue, interval.finalValue)

    val data = new Array[Byte](rawData.capacity())
    rawData.getBytes(0, data)

    println("data into parse method " + new String(data) + ";")

    val envelope = new InputEnvelope(
      key.toString,
      Array((outputs.head, 0)),
      true,
      data
    )

    if (key != 5) key += 1 else key = 1

    Some(envelope)
  }
}