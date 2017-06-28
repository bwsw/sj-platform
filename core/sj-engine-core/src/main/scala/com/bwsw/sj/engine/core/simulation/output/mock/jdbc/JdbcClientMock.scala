package com.bwsw.sj.engine.core.simulation.output.mock.jdbc

import java.sql.Connection

import com.bwsw.common.jdbc.{IJdbcClient, JdbcClientConnectionData}
import com.bwsw.sj.engine.core.simulation.output.JdbcRequestBuilder
import org.mockito.Mockito.{mock, when}

/**
  * Mock of [[IJdbcClient]] for for [[JdbcRequestBuilder]]
  *
  * @param table name of SQL-table
  * @author Pavel Tomskikh
  */
class JdbcClientMock(table: String) extends IJdbcClient {
  override protected var _connection: Option[Connection] = Some(new JdbcConnectionMock)
  override val jdbcCCD: JdbcClientConnectionData = mock(classOf[JdbcClientConnectionData])
  when(jdbcCCD.table).thenReturn(Some(table))
}
