package com.bwsw.sj.common.DAL.service

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.DAL.model.ConfigElement

/**
 * Provides a service for access to elements of configuration file that keeps in mongo database
 */

class ConfigFileService(fileStorage: MongoFileStorage) extends GenericMongoService[ConfigElement] {

  def getFile(name: String, tempFileName: String) = {
    val fileName = super.get(name).value
    fileStorage.get(fileName, tempFileName)
  }
}
