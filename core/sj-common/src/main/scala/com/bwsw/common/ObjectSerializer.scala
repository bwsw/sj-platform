package com.bwsw.common

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.slf4j.LoggerFactory

/**
 * Class represents a serializer from object to byte array and vice versa
 *
 * @author Kseniya Mikhaleva
 */

class ObjectSerializer {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def serialize(obj: Object): Array[Byte] = {
    logger.debug(s"Serialize an object of class: '${obj.getClass}' to a byte array.")
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(obj)
    byteArrayOutputStream.toByteArray
  }

  def deserialize(bytes: Array[Byte]): Object = {
    logger.debug(s"Deserialize a byte array to an object.")
    val b = new ByteArrayInputStream(bytes)
    val o = new ObjectInputStream(b)
    o.readObject()
  }
}
