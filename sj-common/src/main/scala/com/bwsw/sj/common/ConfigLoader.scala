package com.bwsw.sj.common

import java.io.File

import com.typesafe.config.ConfigFactory

object ConfigLoader {
  def load() = {
    val config = ConfigFactory.load()
    if (!config.hasPath("appconf")) {
      config
    } else {
      ConfigFactory.parseFile(new File(config.getString("appconf")))
    }
  }
}
