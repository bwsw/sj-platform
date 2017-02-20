package com.bwsw.sj.common.engine


trait EnvelopeDataSerializer[T <: AnyRef] {
  def deserialize(bytes: Array[Byte]): T

  def serialize(data: T): Array[Byte]
}
