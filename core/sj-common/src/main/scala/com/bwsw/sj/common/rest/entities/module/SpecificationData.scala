package com.bwsw.sj.common.rest.entities.module

case class SpecificationData(var name: String,
                               var description: String,
                               var version: String,
                               var author: String,
                               var license: String,
                               inputs: Map[String, Any],
                               outputs: Map[String, Any],
                               var moduleType: String,
                               var engineName: String,
                               var engineVersion: String,
                               options: Map[String, Any],
                               var validateClass: String,
                               executorClass: String)
