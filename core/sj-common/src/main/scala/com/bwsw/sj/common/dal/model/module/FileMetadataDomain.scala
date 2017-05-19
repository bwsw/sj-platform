package com.bwsw.sj.common.dal.model.module

import java.util.Date

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.IdField
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Entity

/**
  * Domain entity for file metadata (stored in fs.files collection)
  */

@Entity("fs.files")
case class FileMetadataDomain(@IdField _id: ObjectId,
                              name: String,
                              filename: String,
                              filetype: String,
                              uploadDate: Date,
                              length: Long,
                              specification: SpecificationDomain
                             )