package com.bwsw.sj.common.engine

import scala.collection.mutable.ArrayBuffer

case class ValidationInfo(result: Boolean = true, errors: ArrayBuffer[String] = ArrayBuffer())
