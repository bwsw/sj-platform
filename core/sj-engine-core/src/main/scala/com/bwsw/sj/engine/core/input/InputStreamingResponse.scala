package com.bwsw.sj.engine.core.input

/**
 * Class represents a response that will be sent to client after an input envelope has been processed 
 * or a checkpoint has be done
 * There contains a message and flag denoting
 * whether this message will be buffered or will be sent immediately
 * Created: 21/07/2016
 *
 * @author Kseniya Mikhaleva
 */
case class InputStreamingResponse(message: String, isBuffered: Boolean)
