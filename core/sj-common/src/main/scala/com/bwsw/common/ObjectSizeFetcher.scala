package com.bwsw.common

object ObjectSizeFetcher {
  private val serializer = new ObjectSerializer()

  def getObjectSize(anyRef: AnyRef): Int = {
    serializer.serialize(anyRef).length
  }
}