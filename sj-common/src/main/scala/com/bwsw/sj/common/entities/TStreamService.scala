package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.Reference

class TStreamService extends Service{
  var namespace: String = null
  @Reference var `metadata_provider`: Provider = null
  var `metadata_namespace`: String = null
  @Reference var `data_provider`: Provider = null
  var `data_namespace`: String = null
  @Reference var `lock_provider`: Provider = null
  var `lock_namespace`: String = null
}
