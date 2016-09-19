package com.bwsw.sj.common.rest.entities.module

case class TaskStream(name: String, mode: String, var availablePartitionsCount: Int, var currentPartition: Int = 0)
