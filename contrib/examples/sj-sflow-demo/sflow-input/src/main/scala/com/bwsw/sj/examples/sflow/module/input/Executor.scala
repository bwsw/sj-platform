package com.bwsw.sj.examples.sflow.module.input

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.utils.SflowParser
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.{InputStreamingExecutor, Interval}
import io.netty.buffer.ByteBuf


class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
  val jsonSerializer = new JsonSerializer()
  val sflowStreamName = "sflow"
  val partition = 0

  /**
    * Will be invoked every time when a new part of data is received
    *
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
    *
    * @param buffer Input stream is a flow of bytes
    * @return Input envelope or None
    */
  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope] = {
    val rawData = buffer.slice(interval.initialValue, interval.finalValue)

    val data = new Array[Byte](rawData.capacity())
    rawData.getBytes(0, data)

    val maybeRecord = SflowParser.parse(data)

    maybeRecord match {
      case Some(sflowRecord) =>
        println(sflowRecord.toString + ";")//todo for testing
        val serializedResponse = objectSerializer.serialize(sflowRecord)

        Some(new InputEnvelope(
          sflowRecord.timestamp.toString,
          Array((sflowStreamName, partition)),
          false,
          serializedResponse
        ))
      case None => None
    }
  }
}