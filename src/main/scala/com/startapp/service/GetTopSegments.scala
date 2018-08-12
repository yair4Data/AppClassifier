package com.startapp.service

import Util.{FuncUtil, TextTools}
import com.startapp.spring.Logging
import org.apache.spark.sql.functions._
import org.springframework.web.bind.annotation._

/**
  * Created by yairf on 14/02/2018.
  */

@RestController
class GetTopSegments extends java.io.Serializable with Logging{

  @RequestMapping(value=Array("/getTopSegments"), method = Array(RequestMethod.POST))
  def getTopSegments(
                      //@RequestBody app: String
                    @RequestParam (value = "json", required = true) app: String,
                    @RequestParam (value = "top", required = true) top : Int
                    ): String = {

    val spark = CacheHolder.spark

    import spark.implicits._

    val appToClassify = spark.read.json(Seq(app.filter(_ >= ' ')).toDS())

    val classifygDF = appToClassify.withColumn("segment", FuncUtil.getNull()).withColumn("clean_description",TextTools.preProcessText(col("description")))


    //classifygDF.show(1)

    val prediction = CacheHolder.model.transform(classifygDF).select("rawPrediction","clean_description", "description","appId","prediction","predictedLabel")
      .withColumn("predictedLabelFinal", when(TextTools.isEnglish(col("description")), col("predictedLabel")).otherwise("Not supported language"))


    prediction.select(col("appId"),concat_ws(" ", FuncUtil.get_top_predictions(FuncUtil.densevecToSeq(col("rawPrediction")), lit(top)))
      .alias("topXPredictions"))//.orderBy("appId")
      .coalesce(1).collect().mkString(",")

  }
}