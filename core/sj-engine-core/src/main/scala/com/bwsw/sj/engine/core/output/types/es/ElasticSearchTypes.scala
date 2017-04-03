package com.bwsw.sj.engine.core.output.types.es

import java.util.Base64

import org.apache.commons.lang3.StringEscapeUtils

import scala.util.parsing.json._
import com.bwsw.sj.engine.core.output.{IncompatibleTypeException, BasicType}

abstract class ElasticsearchField[T](name: String, default: T) extends BasicType[String, T](name, default)

/**
  * Created by Ivan Kudryavtsev on 03.03.2017.
  */
class IntegerField(name: String, default: java.lang.Integer = 0) extends ElasticsearchField[java.lang.Integer](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case i: java.lang.Integer => i.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Integer.")
  }
}

class LongField(name: String, default: java.lang.Long = 0L) extends ElasticsearchField[java.lang.Long](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case l: java.lang.Long => l.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Long.")
  }
}

class FloatField(name: String, default: java.lang.Float = 0.0f) extends ElasticsearchField[java.lang.Float](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case f: java.lang.Float => f.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Float.")
  }
}

class DoubleField(name: String, default: java.lang.Double = 0.0) extends ElasticsearchField[java.lang.Double](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case d: java.lang.Double => d.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Double.")
  }
}

class ByteField(name: String, default: java.lang.Byte = 0.toByte) extends ElasticsearchField[java.lang.Byte](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case b: java.lang.Byte => b.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Byte.")
  }
}

class CharField(name: String, default: java.lang.Character = 0.toChar) extends ElasticsearchField[java.lang.Character](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case c: java.lang.Character => c.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Character.")
  }
}

class ShortField(name: String, default: java.lang.Short = 0.toShort) extends ElasticsearchField[java.lang.Short](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case s: java.lang.Short => s.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Short.")
  }
}

class BooleanField(name: String, default: java.lang.Boolean = true) extends ElasticsearchField[java.lang.Boolean](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case b: java.lang.Boolean => b.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Boolean.")
  }
}

class DateField(name: String, default: java.lang.String = "0000-00-00") extends ElasticsearchField[String](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case s: java.lang.String => "\"" + s + "\""
    case l: java.lang.Long => l.toString
    case d: java.util.Date => d.getTime.toString
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be java.lang.String or Long.")
  }
}

class BinaryField(name: String, default: Array[Byte] = new Array[Byte](0)) extends ElasticsearchField[Array[Byte]](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case ab: Array[Byte] => Base64.getEncoder.encodeToString(ab)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be Array[Byte]")
  }
}

class JavaStringField(name: String, default: String = "") extends ElasticsearchField[String](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case s: String => "\"" + StringEscapeUtils.escapeJava(s) + "\""
    case _ => "\"" + StringEscapeUtils.escapeJava(fieldValue.toString) + "\""
  }
}

class HTMLStringField(name: String, default: String = "") extends ElasticsearchField[String](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case s: String => "\"" + StringEscapeUtils.escapeHtml4(s) + "\""
    case _ => "\"" + StringEscapeUtils.escapeHtml4(fieldValue.toString) + "\""
  }
}

class RangeField[T](name: String, default: String = "") extends ElasticsearchField[String](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case (from: java.lang.Integer, to: java.lang.Integer) =>
      s"""{"gte": $from, "lte": $to}"""
    case (from: java.lang.Long, to: java.lang.Long) =>
      s"""{"gte": $from, "lte": $to}"""
    case (from: java.lang.Float, to: java.lang.Float) =>
      s"""{"gte": $from, "lte": $to}"""
    case (from: java.lang.Double, to: java.lang.Double) =>
      s"""{"gte": $from, "lte": $to}"""
    case (from: String, to: String) =>
      s"""{"gte": "${StringEscapeUtils.escapeJava(from)}", "lte": "${StringEscapeUtils.escapeJava(to)}"}"""
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be (Integer, Integer), (Long, Long), (Float, Float) or (String, String).")
  }
}

class ArrayField(name: String, default: String = "[]") extends ElasticsearchField[String](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case s: String =>
      JSON.parseRaw(s).orNull match {
        case _: JSONArray => s
        case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be JSON Array in String form.")
      }
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be JSON Array in String form.")
  }
}

class ObjectField(name: String, default: String = "{}") extends ElasticsearchField[String](name, default) {
  override def transform(fieldValue: Any): String = fieldValue match {
    case null => "null"
    case s: String =>
      JSON.parseRaw(s).orNull match {
        case _: JSONObject => s
        case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be JSON Object in String form.")
      }
    case _ => throw new IncompatibleTypeException(s"Field '$name' has an incompatible type ${fieldValue.getClass.getName}. Must be JSON Object in String form.")
  }
}
