package com.bwsw.sj.examples.pingstation.module.input

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.{InputStreamingExecutor, Interval}
import io.netty.buffer.ByteBuf


class Executor(manager: InputEnvironmentManager) extends InputStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
  val jsonSerializer = new JsonSerializer()
  val echoResponseStreamName = "echo-response"
  val unreachableResponseStreamName = "unreachable-response"
  val partition = 0

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

    val ts = System.currentTimeMillis() / 1000L
    val rawData = buffer.slice(interval.initialValue, interval.finalValue)

    val data = new Array[Byte](rawData.capacity())
    rawData.getBytes(0, data)

    val fpingResponse = new String(data)

    val parsedResponse = fpingResponse.split("\\s+")
    parsedResponse.head match {
      case "ICMP" =>

        val response = new UnreachableResponse(ts, parsedResponse.last)
        println(response + ";") //todo for testing
        val serializedResponse = objectSerializer.serialize(jsonSerializer.serialize(response))

        Some(new InputEnvelope(
          fpingResponse,
          Array((unreachableResponseStreamName, partition)),
          false,
          serializedResponse
        ))
      case ip =>

        val response = new EchoResponse(ts, ip, fpingResponse(5))
        println(response + ";") //todo for testing
        val serializedResponse = objectSerializer.serialize(jsonSerializer.serialize(response))

        Some(new InputEnvelope(
          fpingResponse,
          Array((echoResponseStreamName, partition)),
          false,
          serializedResponse
        ))
    }
  }
}