package com.bwsw.sj.common.dal.model

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.IdField
import org.mongodb.morphia.annotations.Entity
import com.bwsw.sj.common.config.ConfigLiterals

/**
  * Domain entity for config.
  * Domain can be one of the: [[ConfigLiterals.domains]]
  */

@Entity("config")
case class ConfigurationSettingDomain(@IdField name: String, value: String, domain: String)
