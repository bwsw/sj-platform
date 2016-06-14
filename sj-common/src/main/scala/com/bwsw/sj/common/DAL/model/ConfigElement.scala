package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Id, Entity}

/**
 * Entity for one element from configuration file.
 * Configuration file is a whole collection in mongo,
 * collection element is configuration file element.
 */
@Entity("config.file")
class ConfigElement {
  @Id var name: String = null
  var value: String = null

  def this(name: String, value: String) = {
    this()
    this.name = name
    this.value = value
  }
}
