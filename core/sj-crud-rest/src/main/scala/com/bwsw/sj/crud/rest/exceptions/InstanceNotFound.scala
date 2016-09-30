package com.bwsw.sj.crud.rest.exceptions

case class InstanceNotFound(msg: String, key: String) extends Exception(key)
