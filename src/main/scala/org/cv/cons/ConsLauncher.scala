package org.cv.cons

import org.apache.spark.sql.{DataFrame, Encoders, SparkSession}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.TaskContext
import org.apache.spark.api.java.function.MapFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}
import za.co.absa.abris.avro.functions.from_confluent_avro
import za.co.absa.abris.avro.read.confluent.SchemaManager

object ConsLauncher {
  private val logger: Logger = Logger.getLogger(ConsLauncher.getClass)

  def readAvro(dataFrame: DataFrame, schemaRegistryConfig: Map[String, String]): DataFrame = {
    dataFrame.select(from_confluent_avro(col("value"), schemaRegistryConfig) as 'data).select("data.*")
  }

  def updateCameraStates(key:String, values: Iterator[CameraData], state: GroupState[CameraData]): CameraData ={
    logger.warn("CameraId=" + key + " PartitionId=" + TaskContext.getPartitionId())
    var existing: CameraData = null
    if (state.exists)
      existing = state.get
    val processed = MotionDetector.detectMotion(key, values, "/home/prabhu.roshan/Downloads/CVApp-Output/", existing)
    if (processed != null)
      state.update(processed)
    processed
  }
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().appName("CV-App-Consumer").master("local[*]").getOrCreate()
    Logger.getLogger("org").setLevel(Level.OFF)
    import spark.implicits._
    val processedImageDir = ""
    val schemaRegistryConfs = Map(
      SchemaManager.PARAM_SCHEMA_REGISTRY_URL -> "http://localhost:8081",
      //SchemaManager.PARAM_KEY_SCHEMA_NAMING_STRATEGY -> SchemaManager.SchemaStorageNamingStrategies.TOPIC_NAME,
      SchemaManager.PARAM_SCHEMA_REGISTRY_TOPIC -> "videoframe",
      SchemaManager.PARAM_VALUE_SCHEMA_NAMING_STRATEGY -> SchemaManager.SchemaStorageNamingStrategies.TOPIC_NAME,
      SchemaManager.PARAM_VALUE_SCHEMA_ID -> "latest"
    )

    val streamDF = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("kafka.max.partition.fetch.bytes", "2097152")
      .option("kafka.max.poll.records", "500")
      .option("subscribe", "videoframe")
      .option("startingOffsets", "earliest")
      .load()

    //streamDF.printSchema()

    val sdf = readAvro(streamDF, schemaRegistryConfs)

    val sdfd =sdf.withColumn("TimeStamp_new",to_timestamp($"TimeStamp")).drop($"TimeStamp").withColumnRenamed("TimeStamp_new","TimeStamp")

    val sds = sdfd.as[CameraData]

    val kvDataSet = sds.groupByKey(new MapFunction[CameraData, String] {
      @throws[Exception]
      override def call(camData: CameraData): String = camData.CameraId
      }, Encoders.STRING)

    val processedData =kvDataSet.mapGroupsWithState(GroupStateTimeout.ProcessingTimeTimeout)(updateCameraStates)
    processedData.writeStream
      .outputMode("update")
      .format("console")
      .option("truncate", "false")
      .start
      .awaitTermination
   /* val processedData = kvDataSet.mapGroupsWithState(GroupStateTimeout.ProcessingTimeTimeout)(new MapGroupsWithStateFunction[String, CameraData, CameraData, CameraData] {
      @throws[Exception]
      override def call(key: String, values: util.Iterator[CameraData], state: GroupState[CameraData]) = {
        logger.warn("CameraId=" + key + " PartitionId=" + TaskContext.getPartitionId())
        var existing: CameraData = CameraData.apply("", 0, 0, 0, null, "")
        if (state.exists)
          existing = state.get
        val processed = MotionDetector.detectMotion(key, values, processedImageDir, existing)
        if (processed != null)
          state.update(processed)
        processed
      }
    }, Encoders.bean(CameraData.getClass))
*/
    //.from_Confluent_Avro("column_containing_avro_data", None, Some(schemaRegistryConfs))(RETAIN_SELECTED_COLUMN_ONLY) // invoke the library passing over parameters to access the Schema Registry


    /*    val schemaRegUrl = "http://localhost:8081"
        val client = new CachedSchemaRegistryClient(schemaRegUrl, 100)
        val deserializer = new KafkaAvroDeserializer(client).asInstanceOf[Deserializer[GenericRecord]]

        import spark.implicits._*/
    /*  val df = spark
        .readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", "localhost:9092")
        .option("subscribe", "videoframe")
        .load()
        .select(
            from_avro($"key", schemaRegUrl).as("key"),
            from_avro($"value", schemaRegUrl).as("value"))



df.printSchema()
*/


    /*val df = spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "staticvideo")
      .option("startingOffsets", "earliest").load()
    df.printSchema()
    val results = df.select(col("value").as[Array[Byte]]).map { rawBytes: Array[Byte] =>
        //read the raw bytes from spark and then use the confluent deserializer to get the record back
        val decoded = deserializer.deserialize("staticvideo", rawBytes)
        val recordId = decoded.get("CameraID").asInstanceOf[org.apache.avro.util.Utf8].toString

        //perform other transformations here!!
        recordId

    }
  results.printSchema()
  results.writeStream
    .outputMode("append")
    .format("console")
    .option("truncate", "false")
    .start()
    .awaitTermination()*/


    /*val avroSchema = new String(Files.readAllBytes(Paths.get("/Users/prabhu.roshan/IdeaProjects/CVApp/src/main/resources/avro/camera-v1.avsc")))
    val videoDf = df.select(from_avro(col("value"), avroSchema).as("video")).select("video.*")

    videoDf.printSchema()*/
  }
}