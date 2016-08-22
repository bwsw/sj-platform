package com.bwsw.common.exceptions

case class BadRequest(msg: String) extends Exception(msg)
