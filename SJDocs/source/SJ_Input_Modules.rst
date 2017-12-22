.. _Provided_Input_Modules:

Imput Modules Provided by SJ-Platform
============================================

The Stream Juggler Platform offers two examples of TCP Input Module implementation. These are ready-to-use input modules for two most general input data formats: CSV and Regex.

CSV Input Module
--------------------------

This module extends the *InputStreamingExecutor* interface. Its aim is to process CSV lines and create ``InputEnvelope`` instance which saves each line as AvroRecord inside.

This module is provided via Sonatype repository.

Module configuration is located in the ``options`` field of instance configuration (see :ref:`REST_API_Instance_Create`).

.. csv-table:: 
 :header: "Field Name", "Format", "Description", "Example"
 :widths: 15, 10, 25, 40

 "outputStream*", "String", "Name of output stream for Avro Records", "s1" 
 "fallbackStream*", "String", "Name of output stream for incorrect CSV-lines", "s2" 
 "fields*", "List[String]", "Names of record fields", "['f1', 'f2', 'f3']" 
 "lineSeparator*", "String", "String which separates lines", "``\n``" 
 "encoding*", "tring", "Name of input encoding", "UTF-8" 
 "uniqueKey", "List[String]", "Set of field names which uniquely identifies a record (all record fields by default)", "['f1', 'f3']" 
 "fieldSeparator", "String", "A delimiter to use for separating entries (',' by default)", ";" 
 "quoteSymbol", "String", "A character to use for quoted elements ('\' by default)", ``*``
 "distribution", "List[String]",  "Set of fields that define in which partition of output stream will be put record. Partition computed as ``hash(fields) mod partitions_number``. If this field not defined, module uses Round Robin policy for partition distribution.", "['f2', 'f3']"

.. note:: `*` - required field.

This module puts `org.apache.avro.generic.GenericRecord <https://avro.apache.org/docs/1.8.1/api/java/org/apache/avro/generic/GenericRecord.html>`_ in output streams. The executor of the next module must take Record type as a parameter, e.g::

 class Executor(manager: ModuleEnvironmentManager) extends BatchStreamingExecutor[Record](manager) {
 ...
 }

In the executor of the next module the Avro Schema (``org.apache.avro.Schema``) and the overridden ``deserialize`` method for deserialization of ``org.apache.avro.generic.GenericRecord`` must be defined . In the ``deserialize`` method the ``deserialize(bytes : Array[Byte], schema : org.apache.avro.Schema)`` method from the ``AvroSerializer`` class could be used.

E.g. for ``"fields": ["f1", "f2", "f3"]``)::

 val schema = SchemaBuilder.record("csv").fields()
  .name("f1").`type`().stringType().noDefault()
  .name("f2").`type`().stringType().noDefault()
  .name("f3").`type`().stringType().noDefault()
  .endRecord()
 val serializer = new AvroSerializer

 override def deserialize(bytes: Array[Byte]): GenericRecord = serializer.deserialize(bytes, schema)


Regex Input Module
--------------------------

This module extends the *InputStreamingExecutor* interface. Its aim is to process input stream of strings using a set of regular expressions rules and create `InputEnvelope` instance which stores each line as AvroRecord inside. Thus, it takes the free-form data, filter and convert them into Avro Records.

This module is provided via Sonatype repository.

**Policy**

Regex input module uses the following policies:

1. first-match-win
       To each data the regular expressions from the list of rules are applied until the first match is found; then these data are converted into avro record and put into the output stream. The matching process for these data is stopped. 

       If none of the rules is matched, the data are converted to unified fallback avro record and put into the fallback stream.

2. check-every
      To each data portion the regular expressions from the list of rules are applied. When matched, the data are converted to Avro Record and put into the output stream. Matching process will continue using the next rule.
 
      If none of the rules is matched, data are converted to unified fallback avro record and put into the fallback stream.

**Configuration**

Module configuration is located in the "options" field of instance configuration (:ref:`REST_API_Instance_Create`).
The configuration contains a three-tier structure that consists of the following levels: options (0-level), rules (1-level), fields (2-level).

**"options"**

.. csv-table:: 
 :header: "Field Name", "Format", "Description", "Example"
 :widths: 15, 10, 25, 40
 
 "lineSeparator *", "String", "String that separates lines", "``\n``"
 "policy*", "String", "Defines the behavior of the module", "first-match-win"
 "encoding*", "String", "Name of input encoding", "UTF-8"
 "fallbackStream*", "String", "Name of an output stream for lines that are not matched to any regex (from the 'rules' field)", "fallback-output"
 "rules*", "List[Rule]", "List of rules that defines: regex, an output stream and avro record structure", "``-``"

**Rule**

.. csv-table:: 
 :header: "Field Name", "Format", "Description", "Example"
 :widths: 15, 10, 25, 40
 
 "regex*", "String", "Regular expression used to filter and transform input data", "(?<day>[0-3]\d)-(?<month>[0-1]\d)-(?<year>\d{4})"
 "outputStream*", "String", "Name of output stream for successful converted data", "output-stream"
 "uniqueKey", "List[String]", "Set of field names which uniquely identifies a record (all record fields by default)","['day','month']"
 "distribution", "List[String]", "Set of fields that define in which partition of an output stream a record will be put. Partition computed as hash(fields) mod partitions_number. If this field is not defined, the module uses the Round Robin policy for partition distribution.", "['month','year']"
 "fields*", "List[Field]", "List of fields used for creation the avro record scheme", "``-``"

**Field**

.. csv-table:: 
 :header: "Field Name", "Format", "Description", "Example"
 :widths: 15, 10, 25, 40
 
 "name*", "String", "Name of the record field", "day"
 "defaultValue*", "String", "Value that used in case of missing field in data", "01"
 "type*", "String", "Type of the record field [boolean, int, long, float, double, string]", "string"

.. note:: `*` - required fields

Configuration example::

 {
	"lineSeparator": "\n",
	"policy": "first-match-win",
	"encoding": "UTF-8",
	"fallbackStream": "fallback-stream",
	"rules": [{
		"regex": "(?<day>[0-3]\\d)-(?<month>[0-1]\\d)-(?<year>\\d{4})",
		"outputStream": "date-output-stream",
		"uniqueKey": ["day", "month"],
		"distribution": ["month", "year"],
		"fields": [{
			"name": "day",
			"default-value": "01",
			"type": "int"
		}, {
			"name": "month",
			"default-value": "01",
			"type": "int"
		}, {
			"name": "year",
			"default-value": "1970",
			"type": "int"
		}]
	}, {
		"regex": "(?<word>\\w+) (?<digit>\\d+)",
		"fields": [{
			"name": "word",
			"default-value": "abc",
			"type": "string"
		}, {
			"name": "digit",
			"default-value": "123",
			"type": "int"
		}],
		"outputStream": "namedigit-output-stream",
		"uniqueKey": ["word"],
		"distribution": ["word", "digit"]
	}]
 }
 
