package com.bwsw.sj.common.engine

import com.bwsw.common.ObjectSerializer

class DefaultEnvelopeDataSerializer(classLoader: ClassLoader) extends EnvelopeDataSerializer[AnyRef] {
  private val serializer = new ObjectSerializer(classLoader)

  override def deserialize(bytes: Array[Byte]) = {
    serializer.deserialize(bytes)
  }

  override def serialize(data: AnyRef): Array[Byte] = {
    serializer.serialize(data)
  }
}
