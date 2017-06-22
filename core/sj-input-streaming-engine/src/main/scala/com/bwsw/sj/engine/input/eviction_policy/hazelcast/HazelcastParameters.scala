package com.bwsw.sj.engine.input.eviction_policy.hazelcast

import com.bwsw.sj.common.si.model.instance.InputInstance

/**
  * Contains attributes of [[InputInstance]] that used for hazelcast map
  *
  * @author Pavel Tomskikh
  */
case class HazelcastParameters(lookupHistory: Int,
                               asyncBackupCount: Int,
                               backupCount: Int,
                               defaultEvictionPolicy: String,
                               queueMaxSize: Int)

object HazelcastParameters {
  def apply(instance: InputInstance): HazelcastParameters = {
    new HazelcastParameters(
      instance.lookupHistory,
      instance.asyncBackupCount,
      instance.backupCount,
      instance.defaultEvictionPolicy,
      instance.queueMaxSize)
  }
}
