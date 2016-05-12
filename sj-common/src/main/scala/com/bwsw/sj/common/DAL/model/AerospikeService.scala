package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class AerospikeService extends Service {
  @Reference var provider: Provider = null
  var namespace: String = null
}
