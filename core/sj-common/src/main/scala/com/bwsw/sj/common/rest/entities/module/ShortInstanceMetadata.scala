package com.bwsw.sj.common.rest.entities.module

case class ShortInstanceMetadata(var name: String,
                                 var moduleType: String,
                                 var moduleName: String,
                                 var moduleVersion: String,
                                 var description: String,
                                 var status: String,
                                 var restAddress: String)
