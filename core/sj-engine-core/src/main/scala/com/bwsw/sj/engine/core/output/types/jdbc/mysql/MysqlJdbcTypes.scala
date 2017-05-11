package com.bwsw.sj.engine.core.output.types.jdbc.mysql

import java.sql.PreparedStatement

import scala.util.parsing.json.{JSON, JSONArray, JSONObject}

import com.bwsw.sj.engine.core.output.types.jdbc.JdbcField
import com.bwsw.sj.engine.core.output.IncompatibleTypeException

/**
  * Created by Ivan Kudryavtsev on 07.03.2017.
  */

class EnumField(name: String, choices: Set[String], default: String = "") extends JdbcField[String](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = Option(fieldValue) match {
    case None => (ps: PreparedStatement, idx: Int) => ps.setString(idx, null)
    case Some(e: String) =>
      if (choices.contains(e)) {
        (ps: PreparedStatement, idx: Int) => ps.setString(idx, e)
      }
      else
        throw new IncompatibleTypeException(s"Field '$name' has incompatible value. Must include the one from choices $choices attribute.")
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be Set[String].")
  }
}

class SetField(name: String, choices: Set[String], default: Set[String] = Set[String]()) extends JdbcField[Set[String]](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = Option(fieldValue) match {
    case None => (ps: PreparedStatement, idx: Int) => ps.setString(idx, null.asInstanceOf[String])
    case Some(sc: Set[_]) =>
      val s = sc.asInstanceOf[Set[String]]
      if (s.subsetOf(choices))
        (ps: PreparedStatement, idx: Int) => ps.setString(idx, s.mkString(","))
      else
        throw new IncompatibleTypeException(s"Field '$name' has incompatible value. Must be a subset of choices $choices attribute.")
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be Set[String].")
  }
}

class JsonArrayField(name: String, default: String = "[]") extends JdbcField[String](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = Option(fieldValue) match {
    case None => (ps: PreparedStatement, idx: Int) => ps.setString(idx, null)
    case Some(s: String) =>
      JSON.parseRaw(s).orNull match {
        case _: JSONArray => (ps: PreparedStatement, idx: Int) => ps.setString(idx, s)
        case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be JSON Array in String form.")
      }
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be JSON Array in String form.")
  }
}

class JsonObjectField(name: String, default: String = "{}") extends JdbcField[String](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = Option(fieldValue) match {
    case None => (ps: PreparedStatement, idx: Int) => ps.setString(idx, null)
    case Some(s: String) =>
      JSON.parseRaw(s).orNull match {
        case _: JSONObject => (ps: PreparedStatement, idx: Int) => ps.setString(idx, s)
        case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be JSON Object in String form.")
      }
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be JSON Object in String form.")
  }
}
