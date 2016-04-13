package com.bwsw.common.DAL

import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.common.traits.Serializer
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.util.JSON

class GenericMongoDAO[T <: Entity : Manifest](entityCollection: MongoCollection, jsonSerializer: Serializer) extends EntityDAO[T] {
  assert(jsonSerializer.getIgnoreUnknown(), "jsonSerializer should ignore unknown fields!")

  def entityToDBObject(entity: T): DBObject = {
    JSON.parse(jsonSerializer.serialize(entity)).asInstanceOf[DBObject]
  }

  override def create(entity: T): String = {
    if (entityCollection.findOne("name" $eq entity.name).isEmpty) {
      entityCollection += entityToDBObject(entity)
      entity.name
    } else {
      throw new BadRecordWithKey("Entity with this name already exists", entity.name)
    }
  }

  override def deleteAll(): Int = {
    entityCollection.remove(MongoDBObject.empty).getN
  }

  override def update(entity: T): Boolean = {
    if (entityCollection.findOne("name" $eq entity.name).isEmpty) {
      entityCollection.findAndModify(
        "name" $eq entity.name,
        entityToDBObject(entity)).isDefined
    } else {
      throw new BadRecordWithKey("Entity with this name already exists", entity.name)
    }
  }

  override def retrieveAll(): Seq[T] = {
    entityCollection.map(o => jsonSerializer.deserialize[T](o.toString)).toSeq
  }

  override def delete(name: String): Boolean = {
    entityCollection.findAndRemove("name" $eq name).isDefined
  }

  override def retrieve(name: String): Option[T] = {
    val entityOption = entityCollection.findOne("name" $eq name).map(_.toString)
    if (entityOption.isDefined) {
      Some(jsonSerializer.deserialize[T](entityOption.get))
    }
    else None
  }
}

