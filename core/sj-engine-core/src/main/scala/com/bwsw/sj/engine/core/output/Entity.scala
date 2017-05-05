package com.bwsw.sj.engine.core.output

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */


class Entity[T] (m: Map[String, NamedType[T]]) {
  def getField(name: String): NamedType[T] = m(name)
  def getFields: Iterable[String] = m.keys
}

class EntityBuilder[T] (m: Map[String, NamedType[T]] = Map[String, NamedType[T]]()) {

  def build(): Entity[T] = new Entity[T](m)

  def field(c: NamedType[T]): EntityBuilder[T] = {
    new EntityBuilder[T](m + (c.getName -> c))
  }
}
