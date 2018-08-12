package com.startapp.service

import Util.TextTools
import com.startapp.service.AppClassifierNaiveNew.{getNull, preProcessText, toArray}
import com.startapp.spring.Logging
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, concat_ws, when}
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, RestController}

/**
  * Created by yairf on 13/02/2018.
  */
@RestController
class FindSegmentForApp extends Logging{


  @RequestMapping(Array("/getBatchSegment"))
  def getBatchSegment
  (//@RequestParam(value = "data", required = true) data: Optional[JsonFileFormat],
   @RequestParam(value = "apps_list", required = false) apps_list: String
  ): String = {

    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .config("spark.sql.warehouse.dir", "file:///+jsonPath")
      .appName("AppClassifier")
      .config("spark.serializer", classOf[KryoSerializer].getName)
      .getOrCreate()

    val appToClassify = spark.sqlContext.read.json("classify.json").cache()/*spark.sqlContext.read.format("csv").option("header", "true").
      option("delimiter", "|").option("inferSchema","true").load("Classify.txt")*/

    //val justEnglishDescToClassify = appToClassify.filter(isEnglish(col("description")))

    //val justEnglishDescClassifyArray = appToClassify.withColumn("descArray",toArray(col("description")))


    //val filteredDescClassify = remover.transform(justEnglishDescClassifyArray)

    //val descToStrClassify = justEnglishDescClassifyArray.withColumn("descArray", concat_ws(",", col("filter_dtopword_description")))

    //TODO withColumn("segment" ..  - to have the same column in the new classified DF - is it needed if no evaluation is done on it ?
    val classifygDF = appToClassify.withColumn("clean_description",preProcessText(col("description")))
      .withColumn("segment", getNull())

    val prediction = CacheHolder.model.transform(classifygDF)//.select("clean_description", "description","appId","prediction","predictedLabel")
      .withColumn("predictedLabelFinal", when(TextTools.isEnglish(col("description")), col("predictedLabel")).otherwise("Not supported language"))


    prediction.createOrReplaceTempView("results")

    val results = spark.sqlContext.sql("select appId,predictedLabelFinal from results")
    
    "["+results.toJSON.collect().toList.mkString(",")+"]"

  }
}
