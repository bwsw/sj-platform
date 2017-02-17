package com.bwsw.sj.common.engine

import com.bwsw.common.ObjectSerializer

class DefaultEnvelopeDataSerializer[T] extends EnvelopeDataSerializer[T] {
  private val serializer = new ObjectSerializer()

  override def deserialize(bytes: Array[Byte]): T = {
    serializer.deserialize(bytes).asInstanceOf[T]
  }

  override def serialize(data: T): Array[Byte] = {
    serializer.serialize(data.asInstanceOf[Object])
  }
}
