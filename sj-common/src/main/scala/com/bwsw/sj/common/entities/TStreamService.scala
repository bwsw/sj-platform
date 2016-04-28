package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.{Property, Reference}

class TStreamService extends Service {
  var namespace: String = null
  @Reference @Property("metadata_provider") var metadataProvider: Provider = null
  @Property("metadata_namespace") var metadataNamespace: String = null
  @Reference @Property("data_provider") var dataProvider: Provider = null
  @Property("data_namespace") var dataNamespace: String = null
  @Reference @Property("lock_provider") var lockProvider: Provider = null
  @Property("lock_namespace") var lockNamespace: String = null
}
