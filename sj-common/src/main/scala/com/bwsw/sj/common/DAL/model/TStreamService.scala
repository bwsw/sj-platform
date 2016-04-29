package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Property

class TStreamService extends Service {
  var namespace: String = null
  @Property("metadata_provider") var metadataProvider: Provider = null
  @Property("metadata_namespace") var metadataNamespace: String = null
  @Property("data_provider") var dataProvider: Provider = null
  @Property("data_namespace") var dataNamespace: String = null
  @Property("lock_provider") var lockProvider: Provider = null
  @Property("lock_namespace") var lockNamespace: String = null
}
