package com.bwsw.sj.output.types.jdbc

import java.sql.PreparedStatement

import com.bwsw.sj.engine.core.output.EntityBuilder
import com.bwsw.sj.engine.core.output.types.jdbc._
import com.mockrunner.jdbc.BasicJDBCTestCaseAdapter
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by diryavkin_dn on 07.03.17.
  */
class JdbcCommandBuilderTests extends FlatSpec with Matchers {
  class  JdbcMock(sql: String) extends BasicJDBCTestCaseAdapter {
    val connection = getJDBCMockObjectFactory.getMockConnection
    val stmt = connection.prepareStatement(sql)
  }

  it should "work properly" in {
    val jdbcMock = new JdbcMock("INSERT ")
    val eb = new EntityBuilder[(PreparedStatement, Int) => Unit]()

    val e = eb
      .field(new LongField("id"))
      .field(new JavaStringField("name"))
      .field(new IntegerField("age"))
      .field(new BooleanField("married", false))
      .build()

    val jdbccb = new JdbcCommandBuilder("txn", e)

    val data = Map[String, Object]().empty +
      ("id" -> new java.lang.Long(0)) +
      ("name" -> "John Smith") +
      ("age" -> new java.lang.Integer(32)) +
      ("married" -> new java.lang.Boolean(true))

    jdbccb.buildInsert(1, data, jdbcMock.stmt).isInstanceOf[PreparedStatement] shouldBe true
  }

  it should "exists work" in {
    val jdbcMock = new JdbcMock("SELECT * FROM table WHERE id = ?")
    val eb = new EntityBuilder[(PreparedStatement, Int) => Unit]()

    val e = eb
      .field(new LongField("id"))
      .field(new JavaStringField("name"))
      .field(new IntegerField("age"))
      .field(new BooleanField("married", false))
      .build()

    val jdbccb = new JdbcCommandBuilder("txn", e)

    jdbccb.exists(1, jdbcMock.stmt) shouldBe false
  }
}
