package com.bwsw.sj.engine.core.input

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import io.netty.buffer.ByteBuf

/**
 * Class that contains an execution logic of input module
 *
 *
 * @author Kseniya Mikhaleva
 */

class InputStreamingExecutor(manager: InputEnvironmentManager) extends StreamingExecutor {
  /**
   * Will be invoked every time when a new part of data is received.
   * This method is dealing with
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
  def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope] = {
    None
  }

  /**
   * Will be invoked after each calling parse method.
   * It is responsible for creating an answer to client about the fact that an envelope is processed
   * @param envelope Input envelope
   * @param isNotEmptyOrDuplicate Flag points whether a processed envelope is empty or duplicate or not.
   *                              If it is true it means a processed envelope is duplicate or empty and false in other case
   * @return A response containing a relevant information about a processed envelope
   */
  def createProcessedMessageResponse(envelope: Option[InputEnvelope], isNotEmptyOrDuplicate: Boolean): InputStreamingResponse = {
    var message = ""
    var isBuffered = true
    if (isNotEmptyOrDuplicate) {
      message = s"Input envelope with key: '${envelope.get.key}' has been sent\n"
      isBuffered = false
    } else if (envelope.isDefined) {
      message = s"Input envelope with key: '${envelope.get.key}' is duplicate\n"
    } else {
      message = s"Input envelope is empty\n"
    }
    InputStreamingResponse(message, isBuffered)
  }

  /**
   * Will be invoked after a checkpoint has been done.
   * It is responsible for creating a response to client about the fact that a checkpoint has been done
   * @return A response containing a relevant information about a done checkpoint
   */
  def createCheckpointResponse(): InputStreamingResponse = {
    InputStreamingResponse(s"Checkpoint has been done\n", isBuffered = false)
  }
}