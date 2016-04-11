package com.bwsw.sj.crud.rest

import java.io._
import java.util.jar.JarFile
import com.bwsw.common.file.utils.FilesStorage
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.FileMetadataDAO
import com.typesafe.config.Config
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONTokener, JSONObject}

/**
  * Trait for validation of crud-rest-api
  * Created: 04/06/2016
  * @author Kseniya Tomskikh
  */
trait SjCrudValidator {
  val conf: Config
  val serializer: Serializer
  val fileMetadataDAO: FileMetadataDAO
  val storage: FilesStorage

  private val moduleTypes = Array("windowed", "regular")

  /**
    * Check String object
    * @param value - input string
    * @return boolean result of checking
    */
  def isEmptyOrNullString(value: String): Boolean = value == null || value.isEmpty

  /**
    * Check specification of uploading jar file
    * @param jarFile - input jar file
    * @return content of specification.json
    */
  def checkJarFile(jarFile: File) = {
    val json = getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      throw new FileNotFoundException(s"Specification.json for ${jarFile.getName} is not found!")
    }
    schemaValidate(json, getClass.getClassLoader.getResourceAsStream("schema.json"))
    serializer.deserialize[Map[String, Any]](json)
  }

  /**
    * Return content of specification.json file from root of jar
    * @param file - Input jar file
    * @return json-string from specification.json
    */
  private def getSpecificationFromJar(file: File): String = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while(enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        try {
          var line = reader.readLine
          while (line != null) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        } finally {
          reader.close()
        }
      }
    }
    builder.toString()
  }

  /**
    * Validate json for such schema
    * @param json input json
    * @param schemaStream schema
    * @return schema is valid
    */
  def schemaValidate(json: String, schemaStream: InputStream): Boolean = {
    if (schemaStream != null) {
      val rawSchema = new JSONObject(new JSONTokener(schemaStream))
      val schema = SchemaLoader.load(rawSchema)
      schema.validate(new JSONObject(json))
    } else {
      throw new Exception("Json schema for specification is not found")
    }
    true
  }

  /**
    * Check existing such type of modules
    * @param typeName name type of module
    * @return true if module type is exist, else false
    */
  def checkModuleType(typeName: String) = {
    moduleTypes.contains(typeName)
  }
}
