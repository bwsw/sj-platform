package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Reference, Property}

class TStreamService extends Service {
  var namespace: String = null
  @Reference(value = "metadata_provider", `lazy` = true) var metadataProvider: Provider = null
  @Property("metadata_namespace") var metadataNamespace: String = null
  @Reference(value = "data_provider", `lazy` = true) var dataProvider: Provider = null
  @Property("data_namespace") var dataNamespace: String = null
  @Reference(value = "lock_provider", `lazy` = true) var lockProvider: Provider = null
  @Property("lock_namespace") var lockNamespace: String = null
}
