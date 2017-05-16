package com.bwsw.sj.common.dal.model

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.IdField
import org.mongodb.morphia.annotations.Entity

/**
  * Entity for one element from configuration settings.
  * Configuration settings is a whole collection in mongo,
  * collection element is one configuration setting.
  */
@Entity("config")
class ConfigurationSettingDomain(@IdField val name: String, val value: String, val domain: String) {
}
