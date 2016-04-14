package com.bwsw.sj.common.DAL

import com.bwsw.common.DAL.GenericMongoDAO
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.entities.{TimeWindowedInstanceMetadata, RegularInstanceMetadata, ShortInstanceMetadata, InstanceMetadata}
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

/**
  * DAO for working with instance collection
  *
  * Created: 13/04/2016
  *
  * @author Kseniya Tomskikh
  * @param entityCollection - mongo db collection
  * @param serializer - data serializer
  */
class InstanceMetadataDAO(entityCollection: MongoCollection, serializer: Serializer)
  extends GenericMongoDAO[InstanceMetadata](entityCollection, serializer) {

  /**
    * Retrieving record from storage by such name
    * record deserializing to object by module-type params
    * @param name - value of name record
    * @return - Some(record) if record for such name exist, else None
    */
  override def retrieve(name: String) = {
    val entityOption = entityCollection.findOne("name" $eq name)
    if (entityOption.isDefined) {
      entityOption.get("module-type").asInstanceOf[String] match {
        case "time-windowed-streaming" => Some(serializer.deserialize[TimeWindowedInstanceMetadata](entityOption.map(_.toString).get))
        case _ => Some(serializer.deserialize[RegularInstanceMetadata](entityOption.map(_.toString).get))
      }
    }
    else None
  }

  /**
    * Retrieve all instancies from storage
    * every record deserializing to object by module-type
    * @return - Set of instancies
    */
  override def retrieveAll() = {
    entityCollection.map{ o =>
      o.get("module-type").asInstanceOf[String] match {
        case "time-windowed-streaming" => serializer.deserialize[TimeWindowedInstanceMetadata](o.toString)
        case _ => serializer.deserialize[RegularInstanceMetadata](o.toString)
      }
    }.toSeq
  }

  /**
    * Retrieve all instancies for such module-type
    * @param moduleName - name of module
    * @param moduleVersion - version of module
    * @param moduleType - name of module type
    * @return - Set of instancies for such module-type
    */
  def retrieveByModule(moduleName: String, moduleVersion: String, moduleType: String) = {
    serializer.setIgnoreUnknown(true)

    entityCollection.find(
      ("module-type" $eq moduleType)
        ++ ("module-name" $eq moduleName)
        ++ ("module-version" $eq moduleVersion)
    ).map(o => serializer.deserialize[ShortInstanceMetadata](o.toString)).toSeq
  }
}
