package com.bwsw.sj.common.engine

import com.bwsw.common.ObjectSerializer
import org.slf4j.LoggerFactory

class DefaultEnvelopeDataSerializer(classLoader: ClassLoader) extends EnvelopeDataSerializer[AnyRef] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val serializer = new ObjectSerializer(classLoader)

  override def deserialize(bytes: Array[Byte]): Object = {
    logger.debug("Deserialize a byte array to an object.")

    serializer.deserialize(bytes)
  }

  override def serialize(data: AnyRef): Array[Byte] = {
    logger.debug(s"Serialize an object of class: '${data.getClass}' to a byte array.")

    serializer.serialize(data)
  }
}
