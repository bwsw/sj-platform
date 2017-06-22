package com.bwsw.sj.engine.input.simulation

import com.bwsw.common.hazelcast.{HazelcastConfig, HazelcastInterface}
import com.hazelcast.core.IMap

/**
  * Mock for [[HazelcastInterface]]
  *
  * @author Pavel Tomskikh
  */
class HazelcastMock(mapName: String, params: HazelcastConfig) extends HazelcastInterface {

  val map = HazelcastMapMock(params)

  /**
    * Returns the hazelcast map
    */
  override def getMap: IMap[String, String] = map
}
