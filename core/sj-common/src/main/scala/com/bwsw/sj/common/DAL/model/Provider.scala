package com.bwsw.sj.common.DAL.model

import java.net.InetSocketAddress

import org.mongodb.morphia.annotations.{Entity, Id, Property}

@Entity("providers")
class Provider {
  @Id var name: String = null
  var description: String = null
  var hosts: Array[String] = null
  var login: String = null
  var password: String = null
  @Property("provider-type") var providerType: String = null

  def this(name: String, description: String, hosts: Array[String], login: String, password: String, providerType: String) = {
    this()
    this.name = name
    this.description = description
    this.hosts = hosts
    this.login = login
    this.password = password
    this.providerType = providerType
  }

  def getHosts() = {
    val inetSocketAddresses = this.hosts.map { host =>
      val parts = host.split(":")
      new InetSocketAddress(parts(0), parts(1).toInt)
    }.toSet

    inetSocketAddresses
  }
}
