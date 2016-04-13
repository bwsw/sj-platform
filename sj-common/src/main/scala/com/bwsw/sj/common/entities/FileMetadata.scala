package com.bwsw.sj.common.entities

import com.bwsw.common.DAL.Entity

case class FileMetadata(var name: String,
                        filename: String,
                        var filetype: String,
                        var metadata: Map[String, Specification]) extends Entity
