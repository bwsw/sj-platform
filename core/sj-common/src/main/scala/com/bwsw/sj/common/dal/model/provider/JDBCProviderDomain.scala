package com.bwsw.sj.common.dal.model.provider

import java.sql.SQLException

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.utils.ProviderLiterals

import scala.collection.mutable.ArrayBuffer

class JDBCProviderDomain(override val name: String,
                         override val description: String,
                         override val hosts: Array[String],
                         override val login: String,
                         override val password: String,
                         val driver: String)
  extends ProviderDomain(name, description, hosts, login, password, ProviderLiterals.jdbcType) {

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

