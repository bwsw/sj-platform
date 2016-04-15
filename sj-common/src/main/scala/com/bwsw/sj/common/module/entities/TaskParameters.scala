package com.bwsw.sj.common.module.entities

import com.bwsw.sj.common.entities.InstanceMetadata

/**
 * Class representing a structure of parameters run the task
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

case class TaskParameters(pathToJar: String,
                          pathToExecutor: String,
                          inputsWithPartitionRange: Map[String, List[Int]],
                          queueSize: Int,
                          transactionTimeout: Int,
                          instanceMetadata: InstanceMetadata)
