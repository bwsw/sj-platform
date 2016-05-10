package com.bwsw.common.exceptions

case class KeyAlreadyExists(msg: String, key: String) extends Exception(msg)