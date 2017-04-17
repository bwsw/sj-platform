package com.bwsw.sj.output.types.jdbc

import java.sql.Connection

import com.bwsw.common.jdbc.{IJdbcClient, JdbcClientConnectionData}
import com.mockrunner.jdbc.BasicJDBCTestCaseAdapter

class JdbcMock extends BasicJDBCTestCaseAdapter with IJdbcClient {
  override protected var _connection: Option[Connection] = Some(getJDBCMockObjectFactory.getMockConnection)
  override val jdbcCCD: JdbcClientConnectionData = new JdbcClientConnectionData(Array("localhost"), "mysql", "login", "password", "database", "table")
}