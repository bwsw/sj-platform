package com.bwsw.sj.crud.rest.exceptions

case class ModuleNotFound(msg: String, key: String) extends Exception(msg)

