package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.sj.engine.core.output.IncompatibleTypeException
import com.bwsw.sj.engine.core.output.types.es._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Ivan Kudryavtsev on 04.03.2017.
  */
class ElasticSearchTypesTests extends FlatSpec with Matchers {
  "IntField" should "return proper values" in {
    val field = new IntegerField("field")
    field.transform(new java.lang.Integer(0)) shouldBe "0"

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
    field.transform(new java.lang.Long(0)) shouldBe "0"

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
    field.transform(new java.lang.Float(0.0f)) shouldBe "0.0"

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
    field.transform(new java.lang.Double(0.0)) shouldBe "0.0"

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
    field.transform(new java.lang.Byte('a'.toByte)) shouldBe "97"

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
    field.transform(new java.lang.Character('a')) shouldBe "a"

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
    field.transform(new java.lang.Short(java.lang.Short.MIN_VALUE)) shouldBe s"${java.lang.Short.MIN_VALUE}"

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
    field.transform(new java.lang.Boolean(true)) shouldBe s"true"

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
    field.transform("0000-00-00") shouldBe s""""0000-00-00""""
    field.transform(new java.lang.Long(0l)) shouldBe s"0"

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe "0000-00-00"

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Integer(0))
    }
  }

  "BinaryField" should "return proper values" in {
    val field = new BinaryField("field")
    field.transform(Array[Byte](0, 1, 2)) shouldBe s"AAEC"

    field.getName shouldBe "field"
    field.getDefaultValue.isInstanceOf[Array[Byte]] shouldBe true

    intercept[IncompatibleTypeException] {
      field.transform(new java.lang.Integer(0))
    }
  }

  "JavaStringField" should "return proper values" in {
    val field = new JavaStringField("field")
    field.transform("""John "Smith""").size - """John "Smith""".size shouldBe 3

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe ""

    field.transform(new java.lang.Integer(0)) shouldBe """"0""""
  }

  "HTMLStringField" should "return proper values" in {
    val field = new HTMLStringField("field")
    field.transform("""John<br/>Smith""") shouldBe """"John&lt;br/&gt;Smith""""

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe ""

    field.transform(new java.lang.Integer(0)) shouldBe """"0""""
  }

  "RangeField" should "return proper values" in {
    val field = new RangeField("field")
    field.transform((new java.lang.Integer(1),new java.lang.Integer(10))) shouldBe """{"gte": 1, "lte": 10}"""
    field.transform((new java.lang.Long(1),new java.lang.Long(10))) shouldBe """{"gte": 1, "lte": 10}"""
    field.transform((new java.lang.Float(1.1),new java.lang.Float(10.1))) shouldBe """{"gte": 1.1, "lte": 10.1}"""
    field.transform((new java.lang.Double(1.1),new java.lang.Double(10.1))) shouldBe """{"gte": 1.1, "lte": 10.1}"""
    field.transform(("2000-01-01","2010-01-01")) shouldBe """{"gte": "2000-01-01", "lte": "2010-01-01"}"""

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe ""
  }

  "ArrayField" should "return proper values" in {
    val field = new ArrayField("field")
    field.transform("[1, 2, 3]") shouldBe """[1, 2, 3]"""

    intercept[IncompatibleTypeException] {
      field.transform("""{"a": 1, "b": 2}""")
    }

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe "[]"
  }

  "ObjectField" should "return proper values" in {
    val field = new ObjectField("field")
    field.transform("""{"a": 1, "b": 2}""") shouldBe """{"a": 1, "b": 2}"""

    intercept[IncompatibleTypeException] {
      field.transform("[]")
    }

    field.getName shouldBe "field"
    field.getDefaultValue shouldBe "{}"
  }


}
