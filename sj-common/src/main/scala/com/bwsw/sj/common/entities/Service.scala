package com.bwsw.sj.common.entities

import com.bwsw.common.DAL.Entity
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Entity for service
  * Created: 22/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class Service(var name: String,
                   @JsonProperty("type") serviceType: String,
                   val description: String,
                   val provider: String,
                   val keyspace: String /*for cassandra*/ ,
                   val index: String /*for ES*/ ,
                   val namespace: String /*for T-Streams, ZK, Redis, Aerospike*/ ,
                   @JsonProperty("metadata_provider") val metadataProvider: String /*for T-Streams*/ ,
                   @JsonProperty("metadata_namespace") val metadataNamespace: String /*for T-Streams*/ ,
                   @JsonProperty("data_provider") val dataProvider: String /*for T-Streams*/ ,
                   @JsonProperty("data_namespace") val dataNamespace: String /*for T-Streams*/ ,
                   @JsonProperty("lock_provider") val lockProvider: String /*for T-Streams*/ ,
                   @JsonProperty("lock_namespace") val lockNamespace: String /*for T-Streams*/) extends Entity