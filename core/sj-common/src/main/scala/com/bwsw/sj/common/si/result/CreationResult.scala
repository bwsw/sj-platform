package com.bwsw.sj.common.si.result

import scala.collection.mutable.ArrayBuffer

trait CreationResult

case object Created extends CreationResult

case class NotCreated(errors: ArrayBuffer[String] = ArrayBuffer()) extends CreationResult
