package com.bwsw.sj.common.engine


trait EnvelopeDataSerializer[T] {
  def deserialize(bytes: Array[Byte]): T

  def serialize(data: T): Array[Byte]
}
