package com.bwsw.common.client

import com.sun.xml.internal.messaging.saaj.soap.Envelope

/**
  * Created by diryavkin_dn on 03.11.16.
  */
trait OutputClient {
  def write()
  def remove()
  def prepare()
}

trait OutputClientConnectionData {

}

object OutputClientBuilder {

}



class JdbcClient() extends OutputClient {
  def write() = {}
  def remove() = {}
  def prepare() = {}
}