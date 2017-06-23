package com.bwsw.sj.engine.input.simulation

import com.bwsw.common.hazelcast.{HazelcastConfig, HazelcastInterface}
import com.hazelcast.core.IMap

/**
  * Mock for [[HazelcastInterface]]
  *
  * @param config configuration parameters for hazelcast cluster
  * @author Pavel Tomskikh
  */
class HazelcastMock(config: HazelcastConfig) extends HazelcastInterface {

  val map = HazelcastMapMock(config)

  /**
    * @inheritdoc
    */
  override def getMap: IMap[String, String] = map
}
