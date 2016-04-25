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
                   val keyspace: String/*for cassandra*/,
                   val index: String/*for ES*/,
                   val namespace: String/*for T-Streams, ZK, Redis, Aerospike*/,
                   @JsonProperty("metadata_service") val metadataService: String/*for T-Streams*/,
                   @JsonProperty("data_service") val dataService: String/*for T-Streams*/,
                   @JsonProperty("lock_service") val lockService: String/*for T-Streams*/) extends Entity