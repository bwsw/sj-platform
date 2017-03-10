package com.bwsw.sj.output.types.jdbc

import java.math.BigDecimal
import java.sql.PreparedStatement

import com.bwsw.sj.engine.core.output.IncompatibleTypeException
import com.bwsw.sj.engine.core.output.types.jdbc._
import com.mockrunner.jdbc.BasicJDBCTestCaseAdapter
import com.mockrunner.mock.jdbc.MockPreparedStatement
import org.scalatest.{FlatSpec, Matchers}


/**
  * Created by diryavkin_dn on 06.03.17.
  */

class JdbcTypesTests extends FlatSpec with Matchers {
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


  "IntField" should "return proper values" in {
    val field = new IntegerField("field")

    jdbcMock.apply(field.transform(new java.lang.Integer(0)))
    jdbcMock.check[java.lang.Integer](0)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe 0

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Long(0))
    }
  }

  "LongField" should "return proper values" in {
    val field = new LongField("field")
    jdbcMock.apply(field.transform(new java.lang.Long(0)))
    jdbcMock.check[java.lang.Long](0L)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe 0

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Integer(0))
    }
  }

  "FloatField" should "return proper values" in {
    val field = new FloatField("field")
    jdbcMock.apply(field.transform(new java.lang.Float(0.0f)))
    jdbcMock.check[java.lang.Float](0.0f)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe 0.0f

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Double(0))
    }
  }

  "DoubleField" should "return proper values" in {
    val field = new DoubleField("field")
    jdbcMock.apply(field.transform(new java.lang.Double(0.0)))
    jdbcMock.check[java.lang.Double](0.0)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe 0.0

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Float(0))
    }
  }

  "ByteField" should "return proper values" in {
    val field = new ByteField("field")
    jdbcMock.apply(field.transform(new java.lang.Byte('a'.toByte)))
    jdbcMock.check[java.lang.Byte]('a'.toByte)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe 0

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Float(0))
    }
  }

  "CharField" should "return proper values" in {
    val field = new CharField("field")
    jdbcMock.apply(field.transform(new java.lang.Character('a')))
    jdbcMock.check[java.lang.String]("a")

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe 0

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Float(0))
    }
  }

  "ShortField" should "return proper values" in {
    val field = new ShortField("field")
    jdbcMock.apply(field.transform(new java.lang.Short(java.lang.Short.MIN_VALUE)))
    jdbcMock.check[java.lang.Short](java.lang.Short.MIN_VALUE)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe 0

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Integer(0))
    }
  }

  "BooleanField" should "return proper values" in {
    val field = new BooleanField("field")
    jdbcMock.apply(field.transform(new java.lang.Boolean(true)))
    jdbcMock.check[java.lang.Boolean](true)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe true

    intercept[IncompatibleTypeException] {
      field.transform("0")
    }

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Integer(0))
    }
  }

  "DateField" should "return proper values" in {
    val field = new DateField("field")
    jdbcMock.apply(field.transform(new java.sql.Date(0L)), 0)
    jdbcMock.check[java.sql.Date](new java.sql.Date(0L))

//    jdbcMock.apply(field.transform("0000-00-00"), 1)
//    jdbcMock.check[java.sql.Date](new java.sql.Date(0L))

    jdbcMock.apply(field.transform(new java.lang.Long(0L)), 2)
    jdbcMock.check[java.sql.Date](new java.sql.Date(0L))

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe new java.sql.Date(0L)

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Integer(0))
    }
  }

  "BinaryField" should "return proper values" in {
    val field = new BinaryField("field")
    jdbcMock.apply(field.transform(Array[Byte](0, 1, 2)))
    jdbcMock.check[Array[Byte]](Array[Byte](0, 1, 2))

    field.getName shouldBe "field"
    field.getDefaultValue.isInstanceOf[Array[Byte]] shouldBe true

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Integer(0))
    }
  }

  "JavaStringField" should "return proper values" in {
    val field = new JavaStringField("field")
    jdbcMock.apply(field.transform("""John "Smith"""), 0)
    var parameterString = jdbcMock.stmt.asInstanceOf[MockPreparedStatement].getParameter(0).asInstanceOf[java.lang.String]
    parameterString.length - """John "Smith""".length shouldBe 1

    jdbcMock.apply(field.transform(new java.lang.Integer(0)), 1)
    parameterString = jdbcMock.stmt.asInstanceOf[MockPreparedStatement].getParameter(1).asInstanceOf[java.lang.String]
    parameterString.length shouldBe 1

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe ""
  }

  "HTMLStringField" should "return proper values" in {
    val field = new HTMLStringField("field")
    jdbcMock.apply(field.transform("""John<br/>Smith"""), 0)
    jdbcMock.check[java.lang.String]("""John&lt;br/&gt;Smith""")

    jdbcMock.apply(field.transform(new java.lang.Integer(0)), 1)
    val parameterString = jdbcMock.stmt.asInstanceOf[MockPreparedStatement].getParameter(1).asInstanceOf[java.lang.String]
    parameterString.length shouldBe 1

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe ""
  }

  "DecimalField" should "return proper values" in {
    val field = new DecimalField("field")
    jdbcMock.apply(field.transform(BigDecimal.TEN), 0)
    jdbcMock.check[BigDecimal](BigDecimal.TEN)

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe BigDecimal.ZERO
  }

}



