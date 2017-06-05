package com.bwsw.sj.output.types.jdbc

import java.sql.Connection

import com.bwsw.common.jdbc.{IJdbcClient, JdbcClientConnectionData}
import com.bwsw.sj.common.config.ConfigLiterals.jdbcDriver
import com.bwsw.sj.common.config.SettingsUtils
import com.mockrunner.jdbc.BasicJDBCTestCaseAdapter
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import scaldi.{Injector, Module}

class JdbcMock extends BasicJDBCTestCaseAdapter with IJdbcClient {
  private val settingsUtilsMock = new SettingsUtilsMock()

  override protected var _connection: Option[Connection] = Some(getJDBCMockObjectFactory.getMockConnection)
  override val jdbcCCD: JdbcClientConnectionData = new JdbcClientConnectionData(
    Array("localhost"),
    "mysql",
    "login",
    "password",
    Option("database"),
    Option("table")
  )(settingsUtilsMock.injector)
}

class SettingsUtilsMock extends MockitoSugar {
  private val settingsUtils = mock[SettingsUtils]

  when(settingsUtils.getJdbcDriverFileName(anyString())).thenReturn(s"$jdbcDriver.driver-name")
  when(settingsUtils.getJdbcDriverClass(anyString())).thenReturn(s"$jdbcDriver.driver-name.class")
  when(settingsUtils.getJdbcDriverPrefix(anyString())).thenReturn(s"$jdbcDriver.driver-name.prefix")

  private val module = new Module {
    bind[SettingsUtils] to settingsUtils
  }

  val injector: Injector = module.injector
}