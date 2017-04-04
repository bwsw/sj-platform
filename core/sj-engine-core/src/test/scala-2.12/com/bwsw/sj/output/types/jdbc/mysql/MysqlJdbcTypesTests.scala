package com.bwsw.sj.output.types.jdbc.mysql

import java.sql.PreparedStatement

import com.bwsw.sj.engine.core.output.IncompatibleTypeException
import com.bwsw.sj.engine.core.output.types.jdbc.mysql._
import com.mockrunner.jdbc.BasicJDBCTestCaseAdapter
import com.mockrunner.mock.jdbc.MockPreparedStatement
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Ivan Kudryavtsev on 07.03.2017.
  */
class MysqlJdbcTypesTests extends FlatSpec with Matchers {
  object jdbcMock extends BasicJDBCTestCaseAdapter {
    val connection = getJDBCMockObjectFactory.getMockConnection
    val stmt = connection.prepareStatement("")
    var index: Int = 0
    def check[T](value: T) = {
      stmt.asInstanceOf[MockPreparedStatement].getParameter(index) shouldBe value
    }
    def apply(func: (PreparedStatement, Int) => Unit, index: Int = 0) = {
      this.index = index
      func(stmt, index)
    }
  }

  "EnumField" should "work properly" in {
    val field = new EnumField("field", choices = Set("1","2","3"))

    jdbcMock.apply(field.transform("1"))
    jdbcMock.check[String]("1")

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe ""

    intercept[IncompatibleTypeException] {
      field.transform("4")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Long(0))
    }
  }

  "SetField" should "work properly" in {
    val field = new SetField("field", choices = Set("1","2","3"))

    jdbcMock.apply(field.transform(Set("1")))
    jdbcMock.check[String]("1")

    jdbcMock.apply(field.transform(Set("2","3")))
    jdbcMock.check[String]("2,3")

    jdbcMock.apply(field.transform(Set[String]()))
    jdbcMock.check[String]("")

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe Set[String]()

    intercept[IncompatibleTypeException] {
      field.transform("4")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Long(0))
    }
  }
}
