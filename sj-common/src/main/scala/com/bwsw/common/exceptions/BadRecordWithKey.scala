package com.bwsw.common.exceptions

case class BadRecordWithKey(msg: String, key: String) extends Exception(msg)

