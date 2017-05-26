package com.bwsw.sj.common.si

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.repository.GenericMongoRepository

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Provides methods to access entities in [[GenericMongoRepository]]
  *
  * @tparam M type of entities
  * @tparam T type of domain entities, storing in [[GenericMongoRepository]]
  */
trait ServiceInterface[M, T] {
  protected val serializer = new JsonSerializer()
  protected val entityRepository: GenericMongoRepository[T]

  /**
    * Saves entity to [[entityRepository]]
    *
    * @param entity
    * @return Right(true) if entity saved, Right(false) or Left(errors) if some errors happened
    */
  def create(entity: M): Either[ArrayBuffer[String], Boolean]

  def getAll(): mutable.Buffer[M]

  def get(name: String): Option[M]

  /**
    * Deletes entity from [[entityRepository]] by name
    *
    * @param name name of entity
    * @return Right(true) if entity deleted, Right(false) if entity not found in [[entityRepository]],
    *         Left(error) if some error happened
    */
  def delete(name: String): Either[String, Boolean]
}
