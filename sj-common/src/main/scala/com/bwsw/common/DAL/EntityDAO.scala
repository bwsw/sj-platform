package com.bwsw.common.DAL

trait EntityDAO[T] {
  def create(entity: T): String

  def retrieve(name: String): T

  def update(entity: T): Boolean

  def delete(name: String): Boolean

  def retrieveAll(): Seq[T]

  def deleteAll(): Int

}
