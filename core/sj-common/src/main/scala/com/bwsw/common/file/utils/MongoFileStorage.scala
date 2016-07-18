package com.bwsw.common.file.utils

import java.io.File

import com.bwsw.common.exceptions.BadRecordWithKey
import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.gridfs.Imports._

class MongoFileStorage(mongoDB: MongoDB) extends FileStorage {

  private val gridFS: GridFS = GridFS(mongoDB)

  override def put(file: File, fileName: String): Unit = {
    logger.debug(s"Try to put a file: '$fileName' in a mongo storage: ${mongoDB.name}")
    logger.debug(s"Check whether a mongo storage already contains a file with name: '$fileName' or not")
    if (gridFS.findOne(fileName).isEmpty) {
      logger.debug(s"Create file in a mongo storage: '$fileName'")
      val gridFsFile = gridFS.createFile(file)
      gridFsFile.filename = fileName
      gridFsFile.save()
    } else {
      logger.error(s"File with name: '$fileName' already exists in a mongo storage")
      throw BadRecordWithKey(s"$fileName already exists", fileName)
    }
  }

  override def put(file: File, fileName: String, specification: Map[String, Any], filetype: String) = {
    logger.debug(s"Try to put a file: '$fileName' with a specification in a mongo storage: ${mongoDB.name}")
    logger.debug(s"Check whether a mongo storage already contains a file with name: '$fileName' or not")
    if (gridFS.findOne(fileName).isEmpty) {
      logger.debug(s"Create file in a mongo storage: '$fileName'")
      val gridFsFile = gridFS.createFile(file)
      logger.debug(s"Add a specification to file: '$fileName'")
      gridFsFile.put("specification", specification)
      logger.debug(s"Add a file type to file: '$fileName'")
      gridFsFile.put("filetype", filetype)
      gridFsFile.save()
      //gridFsFile.validate() sometimes mongodb can't get executor for query and fail as no md5 returned from server
    } else {
      logger.error(s"File with name: '$fileName' already exists in a mongo storage")
      throw BadRecordWithKey(s"$fileName already exists", fileName)
    }
  }

  override def get(fileName: String, newFileName: String): File = {
    logger.debug(s"Try to get a file: '$fileName' from a mongo storage: ${mongoDB.name}")
    val storageFile = gridFS.findOne(fileName)
    logger.debug(s"Check whether a mongo storage contains a file with name: '$fileName' or not")
    if (storageFile.isDefined) {
      logger.debug(s"Copy a file: '$fileName' from a mongo storage to temp file with name: '$newFileName'")
      val localFile = File.createTempFile(newFileName, "")
      localFile.deleteOnExit()
      if (storageFile.get.writeTo(localFile) > 0) localFile
      else {
        logger.error(s"MongoFileStorage.get file: '$fileName' failed")
        throw BadRecordWithKey(s"MongoFileStorage.get $fileName failed", fileName)
      }
    } else {
      logger.error(s"File with name: '$fileName' doesn't exist in a mongo storage")
      throw new BadRecordWithKey(s"$fileName doesn't exist", fileName)
    }
  }

  override def getContent(path: String): Seq[String] = {
    logger.debug(s"Get a list of contents of a mongo storage directory: '$path'")
    gridFS.iterator.toList.map(_.filename.get).toSeq
  }

  override def delete(fileName: String): Boolean = {
    logger.debug(s"Try to delete a file: '$fileName' from a mongo storage: ${mongoDB.name}")
    logger.debug(s"Check whether a mongo storage contains a file with name: '$fileName' or not")
    if (gridFS.findOne(fileName).isDefined) {
      logger.debug(s"Delete a file from a mongo storage: '$fileName'")
      gridFS.remove(fileName)
      true
    } else {
      logger.debug(s"Mongo storage doesn't contain a file with name: '$fileName'")
      false
    }
  }

  override def exists(fileName: String): Boolean = {
    logger.debug(s"Check whether a mongo storage contains a file with name: '$fileName' or not")
    gridFS.findOne(fileName).isDefined
  }
}
