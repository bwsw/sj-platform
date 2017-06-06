package com.bwsw.sj.common.si

import java.io.File
import java.util.Date

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.module.{FileMetadataDomain, IOstream, SpecificationDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadataLiterals
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, ModuleMetadataConversion, Specification}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.EngineLiterals.{batchStreamingType, inputStreamingType, outputStreamingType, regularStreamingType}
import com.bwsw.sj.common.utils.{MessageResourceUtils, MessageResourceUtilsMock}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, anyString, argThat, eq => argEq}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable.ArrayBuffer

class ModuleSiTests extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  val tmpDirectory = "/tmp/"

  val moduleMetadataConversion = mock[ModuleMetadataConversion]

  val notStoredModule = createModule("not-stored-module", "v1", regularStreamingType)

  val inputModuleV1 = createModule("input-module", "v1", inputStreamingType)
  val inputModuleV2 = createModule("input-module", "v2", inputStreamingType)
  val inputModules = Seq(inputModuleV1, inputModuleV2)
  val outputModule = createModule("output-module", "v1", outputStreamingType)
  val storedModulesWithJars = Seq(inputModuleV1, inputModuleV2, outputModule)

  val moduleWithoutJar = createModule("module-without-jar", "v3", batchStreamingType, withFile = false)
  val storedModules = storedModulesWithJars :+ moduleWithoutJar

  val fileMetadataRepository = mock[GenericMongoRepository[FileMetadataDomain]]
  when(fileMetadataRepository.getByParameters(any[Map[String, Any]])).thenReturn(Seq.empty)
  when(fileMetadataRepository.getByParameters(Map("filetype" -> FileMetadataLiterals.moduleType)))
    .thenReturn(storedModules.map(_.metadataDomain))
  storedModules.foreach { module =>
    when(fileMetadataRepository.getByParameters(
      Map(
        "filetype" -> FileMetadataLiterals.moduleType,
        "specification.name" -> module.name,
        "specification.module-type" -> module.moduleType,
        "specification.version" -> module.version))).thenReturn(Seq(module.metadataDomain))
  }

  val modulesToType: Map[String, Seq[ModuleInfo]] = Map(
    inputStreamingType -> inputModules,
    regularStreamingType -> Seq.empty,
    batchStreamingType -> Seq(moduleWithoutJar),
    outputStreamingType -> Seq(outputModule))
  modulesToType.foreach {
    case (moduleType, modules) =>
      when(fileMetadataRepository.getByParameters(
        Map(
          "filetype" -> FileMetadataLiterals.moduleType,
          "specification.module-type" -> moduleType)))
        .thenReturn(modules.map(_.metadataDomain))
  }

  val moduleWithoutInstances = inputModuleV1

  val inputInstanceNames = Seq("input-instance-1", "input-instance-2")
  val outputInstanceNames = Seq("output-instance-1", "output-instance-2")

  val modulesWithInstances = Map(
    inputModuleV2 -> inputInstanceNames,
    outputModule -> outputInstanceNames)

  val moduleToInstanceNames = modulesWithInstances + (moduleWithoutInstances -> Seq.empty)

  val instanceRepository = mock[GenericMongoRepository[InstanceDomain]]
  when(instanceRepository.getByParameters(any[Map[String, Any]])).thenReturn(Seq.empty)
  modulesWithInstances.foreach {
    case (module, names) =>
      val instances = names.map { name =>
        val instance = mock[InstanceDomain]
        when(instance.name).thenReturn(name)
        instance
      }
      when(instanceRepository.getByParameters(Map(
        "module-name" -> module.name,
        "module-type" -> module.moduleType,
        "module-version" -> module.version)))
        .thenReturn(instances)
  }

  val fileStorage = mock[MongoFileStorage]

  val connectionRepository = mock[ConnectionRepository]

  when(connectionRepository.getFileStorage).thenReturn(fileStorage)
  when(connectionRepository.getFileMetadataRepository).thenReturn(fileMetadataRepository)
  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepository)

  val injector = new Module {
    bind[ConnectionRepository] to connectionRepository
    bind[MessageResourceUtils] to MessageResourceUtilsMock.messageResourceUtils
    bind[FileBuffer] to mock[FileBuffer]
    bind[ModuleMetadataConversion] to moduleMetadataConversion
  }.injector

  val moduleSI = new ModuleSI()(injector)

  override def beforeEach(): Unit = {
    reset(fileStorage)

    when(fileStorage.exists(anyString())).thenReturn(false)
    when(fileStorage.delete(anyString())).thenReturn(false)
    storedModulesWithJars.foreach { module =>
      when(fileStorage.get(module.filename, tmpDirectory + module.filename)).thenReturn(module.file.get)
      when(fileStorage.exists(module.filename)).thenReturn(true)
      when(fileStorage.delete(module.filename)).thenReturn(true)
    }
  }


  // create
  "ModuleSI" should "create correct module" in {
    when(notStoredModule.metadata.validate()).thenReturn(ArrayBuffer[String]())

    moduleSI.create(notStoredModule.metadata) shouldBe Created
    verify(fileStorage).put(
      argThat[File](file => file.getName == notStoredModule.filename),
      argEq(notStoredModule.filename),
      argEq(notStoredModule.specificationDomain),
      argEq(FileMetadataLiterals.moduleType))
  }

  it should "not create incorrect module" in {
    val errors = ArrayBuffer("Not valid")
    when(notStoredModule.metadata.validate()).thenReturn(errors)

    moduleSI.create(notStoredModule.metadata) shouldBe NotCreated(errors)
    verify(fileStorage, never()).put(
      any[File](),
      anyString(),
      any[SpecificationDomain](),
      anyString())
  }

  // get
  it should "give module if it exists" in {
    storedModulesWithJars.foreach {
      case ModuleInfo(name, version, moduleType, _, _, _, _, metadata, _, _) =>
        moduleSI.get(moduleType, name, version) shouldBe Right(metadata)
    }
  }

  it should "not give module with incorrect module type" in {
    val name = "some-name"
    val version = "some-version"
    val moduleType = "incorrect type"
    val error = "rest.modules.type.unknown:" + moduleType

    moduleSI.get(moduleType, name, version) shouldBe Left(error)
  }

  it should "not give module if it does not exists" in {
    val name = "some-name"
    val version = "some-version"
    val moduleType = regularStreamingType
    val error = s"rest.modules.module.notfound:$moduleType-$name-$version"

    moduleSI.get(moduleType, name, version) shouldBe Left(error)
  }

  it should "not give module if it does not have jar file" in {
    val error = "rest.modules.module.jar.notfound:" + moduleWithoutJar.signature

    moduleSI.get(moduleWithoutJar.moduleType, moduleWithoutJar.name, moduleWithoutJar.version) shouldBe Left(error)
  }

  // getAll
  it should "give all modules from storage" in {
    moduleSI.getAll.toSet shouldBe storedModules.map(_.metadataWithoutFile).toSet
  }

  // getMetadataWithoutFile
  it should "give module metadata without file if it exists" in {
    storedModulesWithJars.foreach {
      case ModuleInfo(name, version, moduleType, _, _, _, _, _, metadataWithoutFile, _) =>
        moduleSI.getMetadataWithoutFile(moduleType, name, version) shouldBe Right(metadataWithoutFile)
    }
  }

  it should "not give module metadata without file with incorrect module type" in {
    val name = "some-name"
    val version = "some-version"
    val moduleType = "incorrect type"
    val error = "rest.modules.type.unknown:" + moduleType

    moduleSI.getMetadataWithoutFile(moduleType, name, version) shouldBe Left(error)
  }

  it should "not give module metadata without file if it does not exists" in {
    val name = "some-name"
    val version = "some-version"
    val moduleType = regularStreamingType
    val error = s"rest.modules.module.notfound:$moduleType-$name-$version"

    moduleSI.getMetadataWithoutFile(moduleType, name, version) shouldBe Left(error)
  }

  it should "not give module metadata without file if it does not have jar file" in {
    val error = "rest.modules.module.jar.notfound:" + moduleWithoutJar.signature

    val gotten = moduleSI.getMetadataWithoutFile(
      moduleWithoutJar.moduleType,
      moduleWithoutJar.name,
      moduleWithoutJar.version)
    gotten shouldBe Left(error)
  }

  // getByType
  it should "give all modules with specific type" in {
    modulesToType.foreach {
      case (moduleType, modules) =>
        moduleSI.getByType(moduleType).map(_.toSet) shouldBe Right(modules.map(_.metadataWithoutFile).toSet)
    }
  }

  it should "tell error if module type is incorrect" in {
    val moduleType = "incorrect type"
    val error = "rest.modules.type.unknown:" + moduleType

    moduleSI.getByType(moduleType) shouldBe Left(error)
  }

  // getRelatedInstances
  it should "give instances related to module" in {
    moduleToInstanceNames.foreach {
      case (ModuleInfo(_, _, _, _, _, _, _, metadata, _, _), instanceNames) =>
        moduleSI.getRelatedInstances(metadata).toSet shouldBe instanceNames.toSet
    }
  }

  // delete
  it should "delete module without related instances" in {
    moduleSI.delete(moduleWithoutInstances.metadata) shouldBe Deleted
  }

  it should "not delete module with related instances" in {
    modulesWithInstances.keySet.foreach {
      case ModuleInfo(_, _, _, signature, _, _, _, metadata, _, _) =>
        val error = "rest.modules.module.cannot.delete:" + signature

        moduleSI.delete(metadata) shouldBe DeletionError(error)
    }
  }

  it should "tell that storage can't delete module file" in {
    val error = "rest.cannot.delete.file:" + moduleWithoutJar.filename

    moduleSI.delete(moduleWithoutJar.metadata) shouldBe DeletionError(error)
  }

  //   exists
  it should "give module metadata if module exists and has jar file" in {
    storedModulesWithJars.foreach {
      case ModuleInfo(name, version, moduleType, _, _, _, _, _, _, metadataDomain) =>
        moduleSI.exists(moduleType, name, version) shouldBe Right(metadataDomain)
    }
  }

  it should "not give module metadata if module does not exists" in {
    val name = "some-name"
    val version = "some-version"
    val moduleType = regularStreamingType
    val error = s"rest.modules.module.notfound:$moduleType-$name-$version"

    moduleSI.exists(moduleType, name, version) shouldBe Left(error)
  }

  it should "not give module metadata if module type is incorrect" in {
    val name = "some-name"
    val version = "some-version"
    val moduleType = "incorrect type"
    val error = "rest.modules.type.unknown:" + moduleType

    moduleSI.exists(moduleType, name, version) shouldBe Left(error)
  }

  it should "not give module metadata if it does not have jar file" in {
    val error = "rest.modules.module.jar.notfound:" + moduleWithoutJar.signature

    val gotten = moduleSI.exists(
      moduleWithoutJar.moduleType,
      moduleWithoutJar.name,
      moduleWithoutJar.version)
    gotten shouldBe Left(error)
  }

  private def createModule(name: String, version: String, moduleType: String, withFile: Boolean = true): ModuleInfo = {
    val signature = s"$moduleType-$name-$version"

    val filename = s"$name-$version.jar"
    val file = {
      if (withFile) Some(new File(getClass.getResource(filename).toURI))
      else None
    }
    val specificationDomain = new SpecificationDomain(
      name, "", version, "", "", mock[IOstream], mock[IOstream], moduleType, "", "", "", "")

    val specification = mock[Specification]
    when(specification.to).thenReturn(specificationDomain)
    when(specification.name).thenReturn(name)
    when(specification.version).thenReturn(version)
    when(specification.moduleType).thenReturn(moduleType)

    val metadata = mock[ModuleMetadata]
    when(metadata.filename).thenReturn(filename)
    when(metadata.specification).thenReturn(specification)
    when(metadata.file).thenReturn(file)
    when(metadata.name).thenReturn(Some(name))
    when(metadata.version).thenReturn(Some(version))
    when(metadata.signature).thenReturn(signature)

    val metadataWithoutFile = mock[ModuleMetadata]
    when(metadataWithoutFile.filename).thenReturn(filename)
    when(metadataWithoutFile.specification).thenReturn(specification)
    when(metadataWithoutFile.file).thenReturn(None)
    when(metadataWithoutFile.name).thenReturn(Some(name))
    when(metadataWithoutFile.version).thenReturn(Some(version))
    when(metadataWithoutFile.signature).thenReturn(signature)

    val metadataDomain = FileMetadataDomain(
      new ObjectId(),
      name,
      filename,
      FileMetadataLiterals.moduleType,
      new Date(),
      0,
      specificationDomain)

    when(moduleMetadataConversion.from(argEq(metadataDomain), any[Option[File]]())(any[Injector]()))
      .thenReturn(metadataWithoutFile)
    when(moduleMetadataConversion.from(argEq(metadataDomain), argEq(file))(any[Injector]()))
      .thenReturn(metadata)

    ModuleInfo(
      name,
      version,
      moduleType,
      signature,
      filename,
      file,
      specificationDomain,
      metadata,
      metadataWithoutFile,
      metadataDomain)
  }

  case class ModuleInfo(name: String,
                        version: String,
                        moduleType: String,
                        signature: String,
                        filename: String,
                        file: Option[File],
                        specificationDomain: SpecificationDomain,
                        metadata: ModuleMetadata,
                        metadataWithoutFile: ModuleMetadata,
                        metadataDomain: FileMetadataDomain)

}
