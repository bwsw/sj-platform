package com.bwsw.sj.common.engine

/**
  * Interface for serialization/deserialization of envelope data
  *
  * @tparam T type of element of envelope
  */

trait EnvelopeDataSerializer[T <: AnyRef] {
  def deserialize(bytes: Array[Byte]): T

  def serialize(data: T): Array[Byte]
}
