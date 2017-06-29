package com.bwsw.common

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

/**
  * Provides methods for serialization/deserialization objects
  *
  * @author Pavel Tomskikh
  */
trait SerializerInterface {

  def serialize(obj: AnyRef): Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(obj)
    byteArrayOutputStream.toByteArray
  }

  def deserialize(bytes: Array[Byte]): AnyRef = {
    val byteArrayInputStream = new ByteArrayInputStream(bytes)
    val objectInputStream = new ObjectInputStream(byteArrayInputStream)
    objectInputStream.readObject()
  }
}
