package com.bwsw.sj.engine.core.input

/**
  * Class represents a response that will be sent to a client after an input envelope has been processed
  * or a checkpoint has been done
  * It contains a message and a flag
  * indicating whether this message will be buffered or will be sent immediately
  *
  * @author Kseniya Mikhaleva
  */
case class InputStreamingResponse(message: String, isBuffered: Boolean)
