package com.bwsw.sj.output.types.es

import com.bwsw.sj.engine.core.output.types.es.ElasticsearchEntityBuilder
import com.bwsw.sj.engine.core.output.types.es._
import org.scalatest.{FlatSpec, Matchers}


/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchEntityBuilderTests extends FlatSpec with Matchers {
  it should "work properly" in {
    val eb = new ElasticsearchEntityBuilder[String]()

    val id = new LongField("id")
    val name = new JavaStringField("name")
    val age = new IntegerField("age")
    val married = new BooleanField("married", false)

    val e = eb.field(id).field(name).field(age).field(married).build()

    e.getField("id").isInstanceOf[LongField] shouldBe true
    e.getField("name").isInstanceOf[JavaStringField] shouldBe true
    e.getField("age").isInstanceOf[IntegerField] shouldBe true
    e.getField("married").isInstanceOf[BooleanField] shouldBe true

    e.getField("id").asInstanceOf[LongField] shouldBe id
    e.getField("name").asInstanceOf[JavaStringField] shouldBe name
    e.getField("age").asInstanceOf[IntegerField] shouldBe age
    e.getField("married").asInstanceOf[BooleanField] shouldBe married

  }
}
