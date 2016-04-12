package com.bwsw.sj.common.module.entities

/**
 * Сlass represents a structure of launch parameters of module
 * Created: 07/04/2016
 * @author Kseniya Mikhaleva
 */

case class LaunchParameters(instanceName: String,
                            description: String,
                            inputs: List[String],
                            outputs: List[String],
                            options: Map[String, Any],
                             stateStorage: String)
