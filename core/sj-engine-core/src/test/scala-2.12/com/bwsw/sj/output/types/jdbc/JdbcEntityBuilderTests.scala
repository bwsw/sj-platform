package com.bwsw.sj.output.types.jdbc

import com.bwsw.sj.engine.core.output.types.jdbc._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by diryavkin_dn on 06.03.17.
  */
class JdbcEntityBuilderTests extends FlatSpec with Matchers {
  it should "work properly" in {
    val jdbcBuilder = new JdbcEntityBuilder[String]()

    val age = new IntegerField("age")

    val jdbcEntity = jdbcBuilder.field(age).build()

    jdbcEntity.getField("age").isInstanceOf[IntegerField] shouldBe true

  }
}
