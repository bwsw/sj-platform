package com.bwsw.sj.common.DAL.model.module

import java.util.Date

import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.IdField
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Entity

@Entity("fs.files")
case class FileMetadata(@IdField _id: ObjectId,
                        name: String,
                        filename: String,
                        filetype: String,
                        uploadDate: Date,
                        length: Long,
                        specification: Specification
                       )
