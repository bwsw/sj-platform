package com.bwsw.sj.crud.rest

import akka.stream.scaladsl.Source
import com.bwsw.sj.common.rest.{ResponseEntity, RestResponse}
import com.bwsw.sj.crud.rest.model.config.ConfigurationSettingApi
import com.bwsw.sj.crud.rest.model.instance.response.InstanceApiResponse
import com.bwsw.sj.crud.rest.model.module.SpecificationApi
import com.bwsw.sj.crud.rest.model.provider.ProviderApi
import com.bwsw.sj.crud.rest.model.service.ServiceApi
import com.bwsw.sj.crud.rest.model.stream.StreamApi

import scala.collection.mutable

case class ConnectionResponseEntity(connection: Boolean = true) extends ResponseEntity

case class TestConnectionResponseEntity(connection: Boolean, errors: String) extends ResponseEntity

case class ProviderResponseEntity(provider: ProviderApi) extends ResponseEntity

case class ProvidersResponseEntity(providers: mutable.Buffer[ProviderApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToProviderResponseEntity(services: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity


case class ServiceResponseEntity(service: ServiceApi) extends ResponseEntity

case class ServicesResponseEntity(services: mutable.Buffer[ServiceApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToServiceResponseEntity(streams: mutable.Buffer[String] = mutable.Buffer(),
                                          instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity


case class StreamResponseEntity(stream: StreamApi) extends ResponseEntity

case class StreamsResponseEntity(streams: mutable.Buffer[StreamApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToStreamResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

case class ConfigSettingsResponseEntity(configSettings: mutable.Buffer[ConfigurationSettingApi] = mutable.Buffer()) extends ResponseEntity

case class ConfigSettingResponseEntity(configSetting: ConfigurationSettingApi) extends ResponseEntity


case class ModuleJar(filename: String, source: Source[akka.util.ByteString, scala.Any]) extends RestResponse

case class ModuleInfo(moduleType: String, moduleName: String, moduleVersion: String, size: Long)

case class ModulesResponseEntity(modules: mutable.Buffer[ModuleInfo] = mutable.Buffer()) extends ResponseEntity

case class RelatedToModuleResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

case class SpecificationResponseEntity(specification: SpecificationApi) extends ResponseEntity

case class ShortInstancesResponseEntity(instances: mutable.Buffer[ShortInstance] = mutable.Buffer()) extends ResponseEntity

case class InstanceResponseEntity(instance: InstanceApiResponse) extends ResponseEntity

case class InstancesResponseEntity(instances: mutable.Buffer[InstanceApiResponse] = mutable.Buffer()) extends ResponseEntity

case class ShortInstance(name: String,
                         moduleType: String,
                         moduleName: String,
                         moduleVersion: String,
                         description: String,
                         status: String,
                         restAddress: String)

case class CustomJar(filename: String, source: Source[akka.util.ByteString, scala.Any]) extends RestResponse

case class CustomJarInfo(name: String, version: String, size: Long)

case class CustomJarsResponseEntity(customJars: mutable.Buffer[CustomJarInfo] = mutable.Buffer()) extends ResponseEntity


case class CustomFile(filename: String, source: Source[akka.util.ByteString, scala.Any]) extends RestResponse

case class CustomFileInfo(name: String, description: String, uploadDate: String, size: Long) extends ResponseEntity

case class CustomFilesResponseEntity(customFiles: mutable.Buffer[CustomFileInfo] = mutable.Buffer()) extends ResponseEntity
