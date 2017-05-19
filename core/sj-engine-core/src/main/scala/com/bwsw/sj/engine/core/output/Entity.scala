package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.utils.EngineLiterals

/**
  * User defined class to keep processed data of [[EngineLiterals.outputStreamingType]] module
  *
  * @param m set of field names and their values
  * @tparam T type of data elements
  * @author Ivan Kudryavtsev
  */

class Entity[T](m: Map[String, NamedType[T]]) {
  def getField(name: String): NamedType[T] = m(name)

  def getFields: Iterable[String] = m.keys
}

/**
  * Build an entity [[Entity]] by setting fields one by one (for convenience)
  *
  * {{{
  * val entityBuilder = new ElasticsearchEntityBuilder()
  * val entity = entityBuilder
  * .field(new DateField("test-date"))
  * .field(new IntegerField("value"))
  * .field(new JavaStringField("string-value"))
  * .build()
  * }}}
  *
  * @param m set of field names and their values
  * @tparam T type of data elements
  * @author Ivan Kudryavtsev
  */
class EntityBuilder[T](m: Map[String, NamedType[T]] = Map[String, NamedType[T]]()) {

  def build(): Entity[T] = new Entity[T](m)

  def field(c: NamedType[T]): EntityBuilder[T] = {
    new EntityBuilder[T](m + (c.getName -> c))
  }
}
