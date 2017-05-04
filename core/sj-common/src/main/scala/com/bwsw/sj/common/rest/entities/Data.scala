package com.bwsw.sj.common.rest.entities

import scala.collection.mutable.ArrayBuffer

trait Data {
  def validate(): ArrayBuffer[String]
}
