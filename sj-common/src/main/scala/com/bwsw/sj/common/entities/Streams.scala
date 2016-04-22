package com.bwsw.sj.common.entities

import com.bwsw.common.DAL.Entity

/**
  * Entity for stream
  * Created: 21/04/2016
  *
  * @author Kseniya Tomskikh
  */
case class Streams(var name: String,
                   description: String,
                   partitions: List[String],
                   service: String,
                   tags: String,
                   generator: List[String]) extends Entity
