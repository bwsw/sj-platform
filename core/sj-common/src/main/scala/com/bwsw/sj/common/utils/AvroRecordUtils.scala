package com.bwsw.sj.common.utils

import org.apache.avro.generic.GenericData.Record

/**
  * Utils for [[Record]]
  *
  * @author Pavel Tomskikh
  */
object AvroRecordUtils {

  /**
    * Returns concatenated fields values from record.
    *
    * @param fieldNames field names
    * @param record     provides fields with their values
    * @return
    */
  def concatFields(fieldNames: Seq[String], record: Record): String =
    fieldNames.foldLeft("") { (acc, field) => acc + "," + record.get(field).toString }

}
