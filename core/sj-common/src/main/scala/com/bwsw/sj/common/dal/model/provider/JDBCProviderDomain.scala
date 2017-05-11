package com.bwsw.sj.common.dal.model.provider

import java.sql.SQLException

import com.bwsw.common.jdbc.JdbcClientBuilder
import com.bwsw.sj.common.utils.ProviderLiterals

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class JDBCProviderDomain(override val name: String,
                         override val description: String,
                         override val hosts: Array[String],
                         override val login: String,
                         override val password: String,
                         val driver: String,
                         override val providerType: String = ProviderLiterals.jdbcType)
  extends ProviderDomain(name, description, hosts, login, password, providerType) {

  override def checkJdbcConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    Try {
      val client = JdbcClientBuilder.
        setHosts(hosts).
        setDriver(driver).
        setUsername(login).
        setPassword(password).
        build()

      client.checkConnectionToDatabase()
    } match {
      case Success(_) =>
      case Failure(ex: SQLException) =>
        ex.printStackTrace()
        errors += s"Cannot gain an access to JDBC on '$address'"
      case Failure(e) => throw e
    }

    errors
  }
}
