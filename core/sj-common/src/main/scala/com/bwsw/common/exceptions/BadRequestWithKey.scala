package com.bwsw.common.exceptions

case class BadRequestWithKey(msg: String, key: String) extends Exception(msg)

