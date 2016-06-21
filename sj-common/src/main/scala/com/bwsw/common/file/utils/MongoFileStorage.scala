package com.bwsw.common.file.utils

import java.io.File

import com.bwsw.common.exceptions.BadRecordWithKey
import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.gridfs.Imports._

class MongoFileStorage(mongoDB: MongoDB) extends FileStorage {

  private val gridFS: GridFS = GridFS(mongoDB)

  override def put(file: File, fileName: String): Unit = {
    if (gridFS.findOne(fileName).isEmpty) {
      val gridFsFile = gridFS.createFile(file)
      gridFsFile.filename = fileName
      gridFsFile.save()
    } else throw BadRecordWithKey(s"$fileName already exists", fileName)
  }

  override def put(file: File, fileName: String, specification: Map[String, Any], filetype: String) = {
    if (gridFS.findOne(fileName).isEmpty) {
      val gridFsFile = gridFS.createFile(file)
      gridFsFile.put("specification", specification)
      gridFsFile.put("filetype", filetype)
      gridFsFile.save()
      //gridFsFile.validate() sometimes mongodb can't get executor for query and fail as no md5 returned from server
    } else throw BadRecordWithKey(s"$fileName already exists", fileName)
  }

  override def get(fileName: String, newFileName: String): File = {

    val storageFile = gridFS.findOne(fileName)
    if (storageFile.isDefined) {
      val localFile = File.createTempFile(newFileName, "")
      localFile.deleteOnExit()
      if (storageFile.get.writeTo(localFile) > 0) localFile
      else throw BadRecordWithKey(s"MongoFileStorage.get $fileName failed", fileName)
    } else throw new BadRecordWithKey(s"$fileName doesn't exist", fileName)
  }

  override def getContent(path: String): Seq[String] = {
    gridFS.iterator.toList.map(_.filename.get).toSeq
  }

  override def delete(fileName: String): Boolean = {
    if (gridFS.findOne(fileName).isDefined) {
      gridFS.remove(fileName)
      true
    } else false
  }

  override def exists(fileName: String): Boolean = {
    gridFS.findOne(fileName).isDefined
  }
}
