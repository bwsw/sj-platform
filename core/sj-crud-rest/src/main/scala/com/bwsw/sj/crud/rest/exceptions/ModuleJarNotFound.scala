package com.bwsw.sj.crud.rest.exceptions

case class ModuleJarNotFound(msg: String, key: String) extends Exception(msg)
