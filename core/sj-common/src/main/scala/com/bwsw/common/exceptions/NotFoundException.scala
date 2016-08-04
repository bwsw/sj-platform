package com.bwsw.common.exceptions

case class NotFoundException(msg: String, key: String) extends Exception(msg)

