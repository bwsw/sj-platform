package com.bwsw.sj.common.engine

import com.bwsw.common.ObjectSerializer

class DefaultEnvelopeDataSerializer extends EnvelopeDataSerializer[AnyRef] {
  private val serializer = new ObjectSerializer()

  override def deserialize(bytes: Array[Byte]) = {
    serializer.deserialize(bytes)
  }

  override def serialize(data: AnyRef): Array[Byte] = {
    serializer.serialize(data)
  }
}
