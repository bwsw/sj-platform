package com.bwsw.sj.common.DAL.morphia

import org.mongodb.morphia.annotations.{Embedded, Id, Property, Reference}

import scala.annotation.meta.field

object MorphiaAnnotations {
  type IdField = Id @field
  type PropertyField = Property @field
  type ReferenceField = Reference @field
  type EmbeddedField = Embedded @field
}
