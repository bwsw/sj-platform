package com.bwsw.sj.common.module.entities

/**
 * Сlass represents a structure of parameters for spark streaming executor
 * Created: 11/04/2016
 * @author Kseniya Mikhaleva
 */

case class ExecutorParameters(sparkMaster: String, appName: String, launchParameters: LaunchParameters)
