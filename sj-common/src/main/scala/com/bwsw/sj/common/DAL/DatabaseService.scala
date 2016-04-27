package com.bwsw.sj.common.DAL

import scala.collection.mutable

trait DatabaseService[T] {
  def save(entity: T)

  def get(name: String): T

  def getByParameters(parameters: Map[String, Any]): mutable.Buffer[T]

  def getAll: mutable.Buffer[T]

  def delete(name: String)

  def deleteAll()
}
