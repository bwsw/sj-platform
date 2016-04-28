//package com.bwsw.sj.common.DAL
//
//import com.bwsw.common.exceptions.BadRecordWithKey
//import com.bwsw.common.traits.Serializer
//import com.bwsw.sj.common.entities.FileMetadata
//import com.mongodb.casbah.Imports._
//import com.mongodb.casbah.MongoCollection
//
///**
// * DAO for working with files collection in mongodb
// *
// * Created: 07/04/2016
// *
// * @author Kseniya Tomskikh
// * @param entityCollection Files mongodb collection
// * @param jsonSerializer Serializer for parse json
// */
//class FileMetadataDAO(entityCollection: MongoCollection, jsonSerializer: Serializer) {
//
//  /**
//   * Return name of file for uploaded module with such type, name and version
//   *
//   * @param name - name of module
//   * @param typeName - type of module
//   * @param version - version of module
//   * @return - file metadata for uploaded module
//   */
//  def retrieve(name: String, typeName: String, version: String) = {
//    val entityOption = entityCollection.findOne(
//      ("metadata.metadata.name" $eq name)
//        ++ ("metadata.metadata.module-type" $eq typeName)
//        ++ ("metadata.metadata.version" $eq version)
//    ).map(_.toString)
//
//    if (entityOption.isDefined) {
//      jsonSerializer.deserialize[FileMetadata](entityOption.get)
//    } else throw new BadRecordWithKey(s"Entity satisfying the key: $name has not been found", name)
//  }
//
//  /**
//   * Retrieve file metadata for such name and version (if file is not a module)
//   * @param name - name of jar
//   * @param version - version of jar
//   * @return - file metadata
//   */
//  def retrieve(name: String, version: String) = {
//    val entityOption = entityCollection.findOne(
//      ("metadata.metadata.name" $eq name)
//        ++ ("metadata.metadata.version" $eq version)
//    ).map(_.toString)
//
//    if (entityOption.isDefined) {
//      jsonSerializer.deserialize[FileMetadata](entityOption.get)
//    } else throw new BadRecordWithKey(s"Entity satisfying the key: $name has not been found", name)
//  }
//
//  /**
//   * Return all modules with such type
//   *
//   * @param typeName - type of module
//   * @return - collection of modules
//   */
//  def retrieveAllByModuleType(typeName: String) = {
//    entityCollection.find("metadata.metadata.module-type" $eq typeName)
//      .map(o => jsonSerializer.deserialize[FileMetadata](o.toString))
//      .toSeq
//  }
//
//  /**
//   * Returns collection  of files from storage by type of file (module, custom)
//   *
//   * @param filetype - type of uploaded file
//   * @return - collection of uploaded files with such type of file
//   */
//  def retrieveAllByFiletype(filetype: String) = {
//    entityCollection.find("filetype" $eq filetype).map(o => jsonSerializer.deserialize[FileMetadata](o.toString)).toSeq
//  }
//}
