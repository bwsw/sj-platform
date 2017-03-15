package com.bwsw.sj.common.utils

import org.apache.avro.generic.GenericData.Record

/**
  *
  * @author Pavel Tomskikh
  */
object AvroUtils {

  /**
    * Returns concatenated fields values from record.
    *
    * @param fieldNames
    * @param record
    * @return
    */
  def concatFields(fieldNames: Seq[String], record: Record): String =
    fieldNames.foldLeft("") { (acc, field) => acc + "," + record.get(field).toString }

}
