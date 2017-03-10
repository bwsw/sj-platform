package com.bwsw.sj.output.types.es

import com.bwsw.sj.engine.core.output.types.es.ElasticsearchEntityBuilder
import com.bwsw.sj.engine.core.output.types.es._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.parsing.json.{JSON, JSONObject}

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchCommandBuilderTests extends FlatSpec with Matchers {
  it should "work properly" in {
    val eb = new ElasticsearchEntityBuilder()

    val e = eb
      .field(new LongField("id"))
      .field(new JavaStringField("name"))
      .field(new IntegerField("age"))
      .field(new BooleanField("married", false))
      .build()

    val escb = new ElasticsearchCommandBuilder("txn", e)

    val data = Map[String, Object]().empty +
      ("id" -> new java.lang.Long(0)) +
      ("name" -> "John Smith") +
      ("age" -> new java.lang.Integer(32)) +
      ("married" -> new java.lang.Boolean(true))

    JSON.parseRaw(escb.buildIndex(1, data)).get.isInstanceOf[JSONObject] shouldBe true

  }
}
