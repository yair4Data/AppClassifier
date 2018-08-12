package com.startapp.service

import Util.{FuncUtil, TextTools}
import com.startapp.spring.Logging
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, concat_ws, when}
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, RestController}

/**
  * Created by yairf on 13/02/2018.
  */
@RestController
class GetBatchSegment extends Logging{


  @RequestMapping(value= Array("/getBatchSegment"), method = Array(RequestMethod.POST))
  def getBatchSegment (@RequestParam (value = "apps_list", required = true) apps_list: String ): String = {

    val spark = CacheHolder.spark

    import spark.implicits._

    val appToClassify = spark.read.json(Seq(apps_list.filter(_ >= ' ')).toDS())
    //val appToClassify = spark.sqlContext.read.json("classify.json").cache()

    //TODO withColumn("segment" ..  - to have the same column in the new classified DF - is it needed if no evaluation is done on it ?
    val classifygDF = appToClassify.withColumn("clean_description",TextTools.preProcessText(col("description")))
      .withColumn("segment", FuncUtil.getNull())

    val prediction = CacheHolder.model.transform(classifygDF)//.select("clean_description", "description","appId","prediction","predictedLabel")
      .withColumn("predictedLabelFinal", when(TextTools.isEnglish(col("description")), col("predictedLabel")).otherwise("Not supported language"))


    prediction.createOrReplaceTempView("results")

    val results = spark.sqlContext.sql("select appId,predictedLabelFinal from results")

    "["+results.toJSON.collect().toList.mkString(",")+"]"

  }
}
