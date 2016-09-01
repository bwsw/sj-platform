package com.bwsw.sj.engine.core.input

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import io.netty.buffer.ByteBuf

/**
 * It is responsible for input module execution logic. Module uses a specific instance to personalize its work.
 * Executor provides the methods, which has default implementation but you can override these methods if it's needed.
 *
 *
 * @author Kseniya Mikhaleva
 */

class InputStreamingExecutor(manager: InputEnvironmentManager) extends StreamingExecutor {
  /**
   * Is invoked every time when a new part of data is received.
   * Inside you have an access to a buffer with incoming data (a flow of bytes).
   * You should return an Interval (interval contains two values: an initial index of buffer and a final index of buffer).
   * Define the interval by yourself assuming that it can either contain a message or not.
   * The method returns None value by default meaning that the method,
   * more truly the rules defined in the method to determine an interval, can't extract an interval.
   *
   */
  def tokenize(buffer: ByteBuf): Option[Interval] = {
    None
  }

  /**
   * Is invoked after each calling tokenize method if tokenize doesn't return None
   * Inside you have an access to a buffer with incoming data (a flow of bytes) and an interval,
   * which has been returned with tokenize method, so you should define what interval contains (message or trash).
   * After that you should create an InputEnvelope with the following parameters:
   * key of an envelope, information about where this envelope will be sent,
   * boolean flag denoting whether an envelope will be checked on duplication or not and a message data,
   * or just return None (if the buffer interval contains trash). This method returns None by default.
   * Also you can indicate in the instance the field 'duplicate-check'
   * to set up a default policy for message checking on duplication and use the InputEnvelope flag only for special cases.
   */
  def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope] = {
    None
  }

  /**
   * Is invoked after each calling parse method.
   * It is responsible for creating an answer to client about the fact that an envelope is processed
   * Inside you have an access to an InputEnvelope (it can be defined or not)
   * and also you have a flag denoting whether an InputEnvelope is defined and isn't a duplicate (flag will be equal to a true value)
   * or an InputEnvelope is a duplicate or empty (flag will be equal to a false value).
   * Accordingly to this flag you can create an InputStreamingResponse with the relevant information to know what was returned with parse method.
   *
   * @param envelope Input envelope
   * @param isNotEmptyOrDuplicate Flag points whether a processed envelope is empty or duplicate or not.
   *                              If it is false it means a processed envelope is duplicate or empty and true in other case
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
   * Is invoked after a checkpoint has been done.
   * It is responsible for creating a response to client about the fact that a checkpoint has been done
   * @return A response containing a relevant information about a done checkpoint
   */
  def createCheckpointResponse(): InputStreamingResponse = {
    InputStreamingResponse(s"Checkpoint has been done\n", isBuffered = false)
  }
}