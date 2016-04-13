package com.bwsw.sj.common.module.entities

/**
 * Class representing a structure of parameters run the task
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

case class TaskParameters(pathToJar: String,
                          pathToExecutor: String,
                          inputsWithPartitions: Map[String, List[Int]],
                          outputs: List[String],
                          stateStorage: String)
