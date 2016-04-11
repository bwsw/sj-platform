name := "sj-common"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-streaming_2.11" % "1.6.1",
  "org.apache.spark" % "spark-streaming-kafka_2.11" % "1.6.1",
  "org.mongodb" % "casbah_2.11" % "3.0.0"
)
