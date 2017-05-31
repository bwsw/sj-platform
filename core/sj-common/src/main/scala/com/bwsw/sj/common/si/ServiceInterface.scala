package com.bwsw.sj.common.si

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.common.si.result.{CreationResult, DeletionResult}

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
    */
  def create(entity: M): CreationResult

  def getAll(): Seq[M]

  def get(name: String): Option[M]

  /**
    * Deletes entity from [[entityRepository]] by name
    *
    * @param name name of entity
    */
  def delete(name: String): DeletionResult
}
