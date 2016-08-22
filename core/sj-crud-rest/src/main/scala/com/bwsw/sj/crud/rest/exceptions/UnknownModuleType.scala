package com.bwsw.sj.crud.rest.exceptions

case class UnknownModuleType(msg: String, key: String) extends Exception(msg)


