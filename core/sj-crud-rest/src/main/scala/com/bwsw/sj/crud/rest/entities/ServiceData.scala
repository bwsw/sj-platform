package com.bwsw.sj.crud.rest.entities

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[CassDBServiceData], name = "CassDB"),
  new Type(value = classOf[EsIndServiceData], name = "ESInd"),
  new Type(value = classOf[KfkQServiceData], name = "KfkQ"),
  new Type(value = classOf[TstrQServiceData], name = "TstrQ"),
  new Type(value = classOf[ZKCoordServiceData], name = "ZKCoord"),
  new Type(value = classOf[RDSCoordServiceData], name = "RDSCoord"),
  new Type(value = classOf[ArspkDBServiceData], name = "ArspkDB"),
  new Type(value = classOf[JDBCServiceData], name = "JDBC")
))
class ServiceData() {
  @JsonProperty("type") var serviceType: String = null
  var name: String = null
  var description: String = null
}

class CassDBServiceData() extends ServiceData() {
  serviceType = "CassDB"
  var provider: String = null
  var keyspace: String = null
}

class EsIndServiceData() extends ServiceData() {
  serviceType = "ESInd"
  var provider: String = null
  var index: String = null
  var login: String = null
  var password: String = null
}

class KfkQServiceData() extends ServiceData() {
  serviceType = "KfkQ"
  var provider: String = null
  @JsonProperty("zk-provider") var zkProvider : String = null
  @JsonProperty("zk-namespace") var zkNamespace : String = null
}

class TstrQServiceData() extends ServiceData() {
  serviceType = "TstrQ"
  @JsonProperty("metadata-provider") var metadataProvider: String = null
  @JsonProperty("metadata-namespace") var metadataNamespace: String = null
  @JsonProperty("data-provider") var dataProvider: String = null
  @JsonProperty("data-namespace") var dataNamespace: String = null
  @JsonProperty("lock-provider") var lockProvider: String = null
  @JsonProperty("lock-namespace") var lockNamespace: String = null
}

class ZKCoordServiceData() extends ServiceData() {
  serviceType = "ZKCoord"
  var namespace: String = null
  var provider: String = null
}

class RDSCoordServiceData() extends ServiceData() {
  serviceType = "RDSCoord"
  var namespace: String = null
  var provider: String = null
}

class ArspkDBServiceData() extends ServiceData() {
  serviceType = "ArspkDB"
  var namespace: String = null
  var provider: String = null
}

class JDBCServiceData() extends ServiceData() {
  serviceType = "JDBC"
  var namespace: String = null
  var provider: String = null
  var login: String = null
  var password: String = null
}
