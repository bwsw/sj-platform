package com.bwsw.common.hazelcast

/**
  * Configuration of hazelcast cluster
  *
  * @author Pavel Tomskikh
  */
case class HazelcastConfig(ttlSeconds: Int,
                           asyncBackupCount: Int,
                           backupCount: Int,
                           evictionPolicy: String,
                           maxSize: Int)
