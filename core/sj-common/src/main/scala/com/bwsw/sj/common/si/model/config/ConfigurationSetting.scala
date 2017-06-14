/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.si.model.config

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.tstreams.env.ConfigurationOptions
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

// TODO ConfigurationSetting, Provider, Service must have one interface
class ConfigurationSetting(val name: String,
                           val value: String,
                           val domain: String) {

  /**
    * Validates configuration setting
    *
    * @return empty array if configuration setting is correct, validation errors otherwise
    */
  def validate()(implicit injector: Injector): ArrayBuffer[String] = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

    val configRepository = inject[ConnectionRepository].getConfigRepository
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          val modelConfigName = ConfigurationSetting.createConfigurationSettingName(this.domain, this.name)
          if (configRepository.get(modelConfigName).isDefined) {
            errors += createMessage("entity.error.already.exists", "Config setting", x)
          }

          if (!validateConfigSettingName(x)) {
            errors += createMessage("entity.error.incorrect.config.name", x)
          }

          if (this.domain == ConfigLiterals.tstreamsDomain && !validateTstreamProperty()) {
            errors += createMessage("entity.error.incorrect.name.tstreams.domain", "Config setting", x)
          }
        }
    }

    // 'value' field
    Option(this.value) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Value")
      case Some(x) =>
        if (x.isEmpty)
          errors += createMessage("entity.error.attribute.required", "Value")
    }

    // 'domain' field
    Option(this.domain) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Domain")
      case Some(x) =>
        if (x.isEmpty)
          errors += createMessage("entity.error.attribute.required", "Domain")
        else {
          if (!ConfigLiterals.domains.contains(x)) {
            errors += createMessage("rest.validator.attribute.unknown.value", "domain", s"$x") + ". " +
              createMessage("rest.validator.attribute.must.one_of", "Domain", ConfigLiterals.domains.mkString("[", ", ", "]"))
          }
        }
    }

    errors
  }

  private def validateTstreamProperty(): Boolean = {
    this.name.contains("producer") || this.name.contains("consumer") || this.name == ConfigurationOptions.Producer.Transaction.distributionPolicy
  }

  def to(): ConfigurationSettingDomain = {
    ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(domain, name), value, domain)
  }
}

class CreateConfigurationSetting {
  def from(configSettingDomain: ConfigurationSettingDomain): ConfigurationSetting = {
    new ConfigurationSetting(
      ConfigurationSetting.clearConfigurationSettingName(configSettingDomain.domain, configSettingDomain.name),
      configSettingDomain.value,
      configSettingDomain.domain
    )
  }
}

object ConfigurationSetting {
  def createConfigurationSettingName(domain: String, name: String): String = {
    domain + "." + name
  }

  /**
    * Removes domain from configuration setting name
    *
    * @param domain
    * @param name configuration setting name with domain
    */
  def clearConfigurationSettingName(domain: String, name: String): String = {
    name.replaceFirst(domain + ".", "")
  }
}