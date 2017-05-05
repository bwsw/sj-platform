package com.bwsw.sj.common.bll

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common._dal.service.GenericMongoRepository

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait Service[T] {
  protected val serializer = new JsonSerializer()
  protected val entityRepository: GenericMongoRepository[T]

  def process(entity: T): Either[ArrayBuffer[String], Boolean]

  def getAll(): mutable.Buffer[T]

  def get(name: String): Option[T]

  def delete(name: String): Either[String, Boolean]
}
