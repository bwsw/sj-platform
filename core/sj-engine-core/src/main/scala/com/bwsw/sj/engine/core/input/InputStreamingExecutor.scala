package com.bwsw.sj.engine.core.input

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import io.netty.buffer.ByteBuf

/**
 * Class that contains an execution logic of input module
 * Created: 10/07/2016
 *
 * @author Kseniya Mikhaleva
 */

class InputStreamingExecutor(manager: InputEnvironmentManager) extends StreamingExecutor {
  /**
   * Will be invoked every time when a new part of data is received
   * @param buffer Input stream is a flow of bytes
   * @return Interval into buffer that probably contains a message or None
   */
  def tokenize(buffer: ByteBuf): Option[Interval] = {
    None
  }

  /**
   * Will be invoked after each calling tokenize method if tokenize doesn't return None
   * @param buffer A part of an input stream, which had been defined with boundaries that returned a tokenize method
   * @return Input envelope or None
   */
  def parse(buffer: ByteBuf): Option[InputEnvelope] = {
    None
  }

  def createProcessedMessageResponse(envelope: Option[InputEnvelope], isNotEmptyOrDuplicate: Boolean): InputStreamingResponse = {
    var message = ""
    var isBuffered = true
    if (isNotEmptyOrDuplicate) {
      println(s"Input envelope with key: '${envelope.get.key}' has been sent\n") //todo for testing
      message = s"Input envelope with key: '${envelope.get.key}' has been sent\n"
      isBuffered = false
    } else if (envelope.isDefined) {
      println(s"Input envelope with key: '${envelope.get.key}' is duplicate\n") //todo for testing
      message = s"Input envelope with key: '${envelope.get.key}' is duplicate\n"
    } else {
      println(s"Input envelope is empty\n") //todo for testing
      message = s"Input envelope is empty\n"
    }
    InputStreamingResponse(message, isBuffered)
  }

  def createCheckpointResponse(): InputStreamingResponse = {
    InputStreamingResponse(s"Checkpoint has been done\n", isBuffered = false)
  }
}