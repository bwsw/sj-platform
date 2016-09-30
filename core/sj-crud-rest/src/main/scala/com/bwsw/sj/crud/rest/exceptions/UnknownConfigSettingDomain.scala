package com.bwsw.sj.crud.rest.exceptions

case class UnknownConfigSettingDomain(msg: String, key: String) extends Exception(msg)
