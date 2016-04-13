package com.bwsw.common.exceptions

/**
  * Created by tomskikh_ka on 4/13/16.
  */
case class InstanceException(msg: String, key: String) extends Exception(msg)
