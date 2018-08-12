package com.startapp.service

import Util.{FuncUtil, TextTools}
import com.startapp.service.AppClassifierNaiveNew.{getNull, preProcessText}
import com.startapp.spring.Logging
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.{DenseVector, SparseVector, Vectors}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.springframework.web.bind.annotation._

/**
  * Created by yairf on 14/02/2018.
  */

@RestController
class GetTopSegmentsTry extends java.io.Serializable with Logging{



  //from https://stackoverflow.com/questions/43926116/spark-ml-naive-bayes-predict-multiple-classes-with-probabilities


  @RequestMapping(value=Array("/getTopSegmentsTry"), method = Array(RequestMethod.POST))
  def getTopSegmentsTry(@RequestBody app: String): String = {

    /*val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      .config("spark.sql.warehouse.dir", "file:///+jsonPath")
      .appName("AppClassifier")
      .config("spark.serializer", classOf[KryoSerializer].getName)
      .getOrCreate()*/

    val spark = CacheHolder.spark

    import spark.implicits._

    val appToClassify = spark.read.json(Seq(app.filter(_ >= ' ')).toDS())

    val classifygDF = appToClassify.withColumn("segment", getNull()).withColumn("clean_description",preProcessText(col("description")))


    classifygDF.show(1)

    val prediction = CacheHolder.model.transform(classifygDF).select("rawPrediction","clean_description", "description","appId","prediction","predictedLabel")
      .withColumn("predictedLabelFinal", when(TextTools.isEnglish(col("description")), col("predictedLabel")).otherwise("Not supported language"))

    prediction.createOrReplaceTempView("results")

    val results = spark.sqlContext.sql("select * from results")

    //results.show(100/*,false*/)




    //change
    /*val assembler = new VectorAssembler()
      .setInputCols(Array("rawPrediction"))
      .setOutputCol("rawPredictionAsVector")

    val output = assembler.transform(prediction)*/
    val gg = prediction.select("rawPrediction").rdd.map(_.getAs[DenseVector]("rawPrediction"))

    //val gg2 = prediction.select(densevecToSeq(col("rawPrediction")).as("rawPredictionASvector"))
    //val gg3 = prediction.select(vecToSeq(col("rawPrediction")).as("rawPredictionASdensevector"))
    prediction.select(col("appId"),concat_ws(" ", FuncUtil.get_top_predictions(/*convertToML*/FuncUtil.densevecToSeq(col("rawPrediction")), lit(10))).alias("topXPredictions")).orderBy("appId")
      .coalesce(1).show()
    //end change

    //prediction.select(col("appId"),concat_ws(" ", get_top_predictions(col("rawPrediction")), lit(10))).alias("topXPredictions")
    //.orderBy("appId")
    //  .coalesce(1).show()

    "sss"
    //"["+results.collect().map(_.get(0)).mkString(",")+"]"*/

  }
}