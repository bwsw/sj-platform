package com.bwsw.sj.common.DAL.model.provider

import java.sql.SQLException

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.rest.entities.provider.JDBCProviderData
import com.bwsw.sj.common.utils.ProviderLiterals

import scala.collection.mutable.ArrayBuffer

class JDBCProvider extends Provider {
  providerType = ProviderLiterals.jdbcType
  var driver: String = _

  def this(name: String, description: String, hosts: Array[String], login: String, password: String, providerType: String, driver: String): Unit = {
    this()
    this.name = name
    this.description = description
    this.hosts = hosts
    this.login = login
    this.password = password
    this.providerType = providerType
    this.driver = driver
  }

  override def asProtocolProvider(): JDBCProviderData = {
    val providerData = new JDBCProviderData(
      this.name,
      this.login,
      this.password,
      this.providerType,
      this.hosts,
      this.driver,
      this.description
    )

    providerData
  }


  override def checkJdbcConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    try {
      val client = JdbcClientBuilder.
        setHosts(hosts).
        setDriver(driver).
        setUsername(login).
        setPassword(password).
        build()

      client.checkConnectionToDatabase()
    } catch {
      case ex: SQLException =>
        ex.printStackTrace()
        errors += s"Cannot gain an access to JDBC on '$address'"
    }

    errors
  }
}
