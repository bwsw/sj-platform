package com.bwsw.sj.common.entities

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{Id, Entity}

@Entity("fs.files")
class FileMetadata() {
  @Id val _id: ObjectId = null
  val name: String = null
  val filename: String = null
  val filetype: String = null
  val metadata: Map[String, Specification] = null
}