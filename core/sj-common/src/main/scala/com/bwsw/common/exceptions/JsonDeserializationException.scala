package com.bwsw.common.exceptions

/**
  * @author Pavel Tomskikh
  */
class JsonDeserializationException(msg: String) extends Exception(msg)

class JsonIncorrectValueException(msg: String) extends JsonDeserializationException(msg)

class JsonUnrecognizedPropertyException(msg: String) extends JsonDeserializationException(msg)

class JsonNotParsedException(msg: String) extends JsonDeserializationException(msg)
