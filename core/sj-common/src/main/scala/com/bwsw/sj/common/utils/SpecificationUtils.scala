package com.bwsw.sj.common.utils

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.module.SpecificationDomain

import scala.util.{Failure, Success, Try}

class SpecificationUtils {
  private val serializer = new JsonSerializer()
  private var maybeSpecification: Option[SpecificationDomain] = None

  /**
    * Retrieves [[SpecificationDomain]] from jar file
    *
    * @param jarFile
    * @return specification
    */
  def getSpecification(jarFile: File): SpecificationDomain = {
    maybeSpecification match {
      case Some(specification) =>

        specification
      case None =>
        val serializedSpecification = getSpecificationFromJar(jarFile)

        serializer.deserialize[SpecificationDomain](serializedSpecification)
    }
  }

  /**
    * Retrieves content of specification.json file from root of jar
    *
    * @param file jar file
    * @return content of specification.json
    */
  def getSpecificationFromJar(file: File): String = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        val result = Try {
          var line = reader.readLine
          while (Option(line).isDefined) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        }
        reader.close()
        result match {
          case Success(_) =>
          case Failure(e) => throw e
        }
      }
    }

    builder.toString()
  }
}
