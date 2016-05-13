package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class KafkaService extends Service {
  @Reference var provider: Provider = null
}
