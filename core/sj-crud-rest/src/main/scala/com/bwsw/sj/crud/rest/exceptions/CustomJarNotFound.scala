package com.bwsw.sj.crud.rest.exceptions

case class CustomJarNotFound(msg: String, key: String) extends Exception(msg)
