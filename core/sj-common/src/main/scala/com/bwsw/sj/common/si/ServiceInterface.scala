package com.bwsw.sj.common.si

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.repository.GenericMongoRepository

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait ServiceInterface[M, T] {
  protected val serializer = new JsonSerializer()
  protected val entityRepository: GenericMongoRepository[T]

  def create(entity: M): Either[ArrayBuffer[String], Boolean]

  def getAll(): mutable.Buffer[M]

  def get(name: String): Option[M]

  def delete(name: String): Either[String, Boolean]
}
