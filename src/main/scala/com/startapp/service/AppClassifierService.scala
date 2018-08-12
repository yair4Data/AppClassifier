package com.startapp.service

import javax.annotation.PostConstruct

import Util.{AsyncExecutable, FuncUtil, TextCleaner, TextTools}
import com.startapp.spring.Logging
import com.twitter.util.{Duration, ScheduledThreadPoolTimer}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.NaiveBayes
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.functions.{col, concat_ws}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, RestController}
import org.springframework.beans.factory.annotation.Value
/**
  * Created by yairf on 13/02/2018.
  */
@RestController
class AppClassifierService extends  AsyncExecutable with Logging {
  private val asyncProcessDuration = Duration.fromSeconds(1200)
  private[this] val timer: ScheduledThreadPoolTimer = new ScheduledThreadPoolTimer(2)


  @Value("${json.file.path}")
  var jsonPath:String = null


  @PostConstruct
  def callInit(): Unit ={
    //init(jsonPath)
  }

  @RequestMapping(Array("/init"))
  def init ( @RequestParam (value = "data", required = true) data: String ,
             @RequestParam(value = "params", required = false) params: String  ): Unit = {

    if (!Option(params).isEmpty) {
      jsonPath = params.toString().split(",")(0)
    }
    println("examples file path was set from : "+jsonPath)

    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      //.master("spark://saas-logs-lab-master.cws.lab:7077")
      //.config("spark.submit.deployMode", "cluster")
      //.config("spark.executor.memory", "24g")
      //.config("spark.driver.memory", "2g")
      //.config("spark.sql.warehouse.dir", "/var/tmp")
      .config("spark.sql.warehouse.dir", "file:///"+jsonPath)//C:\\Workspace-saas-logs-ng\\AppClassifier")
      .appName("AppClassifier")
      .config("spark.serializer", classOf[KryoSerializer].getName)
      //.config("spark.driver.maxResultSize", "2G")
      /*.config("spark.submit.deployMode", deployMode)
      .config("spark.executor.memory", executorMemory)
      .config("spark.driver.memory", driverMemory)*/
      .getOrCreate()


    CacheHolder.spark_=(spark)  // stor for later use on other services
    val sqlContext = spark.sqlContext

    import spark.implicits._

    //loads examples json
    val appDescriptions = spark.read.json(Seq(data.filter(_ >= ' ')).toDS())

    //val appDescriptions = sqlContext.read.json("example.json").cache()

    cleanInputText(appDescriptions)

    val model = buildClassificationModel(spark)

    CacheHolder.model_=(model)

    //TODO block all other API's until the model is loaded
    val modelAnswer = async[Option[String]] {
      try {
        logger.info(s"Running testModel ")
        Some(testModel())
      } catch {
        case ex: Exception =>
          logger.error(s"Failed with error => ${ex.toString}", ex)
          None
      }
    }

   // modelAnswer.within(timer, asyncProcessDuration)

    modelAnswer.onSuccess(modelAnswer => {
      if (modelAnswer.isDefined) {
        logger.info(s"model Results are : $modelAnswer")
      } else {
        logger.warn("Empty answer arrived :(")
      }
    }).onFailure(ex => {
      logger.error(s"onFailure with error => ${ex.toString}", ex)
    })

  }

  def cleanInputText(trainingData: DataFrame): Unit ={
    val justEnglishDesc = trainingData.filter(TextTools.isEnglish(col("description")))
    val justEnglishDescArray = justEnglishDesc.withColumn("descArray",FuncUtil.toArray(col("description")))

   /* val remover = new StopWordsRemover()
      .setInputCol("descArray")
      .setOutputCol("filter_dtopword_description")*/

    //val filteredDesc = remover.transform(justEnglishDescArray)

    val descToStr = justEnglishDescArray.withColumn("filter_str_description", concat_ws(",", col("descArray")))
    val trainingDF = descToStr.withColumn("clean_description",TextTools.preProcessText(col("filter_str_description"))).drop(col("descArray")).drop(col("filter_str_description")).drop(col("filter_dtopword_description"))

    CacheHolder.appDescriptions_=(trainingDF)
  }

  def buildClassificationModel(spark: SparkSession): PipelineModel ={

    val trainingDF = CacheHolder.appDescriptions
    val indexer = new StringIndexer().setInputCol("segment").setOutputCol("label").fit(trainingDF)
    //breaking the description into individual terms
    val tokenizer = new Tokenizer().setInputCol("clean_description").setOutputCol("tokens")
    val hashingTF = new HashingTF().setInputCol("tokens").setOutputCol("features").setNumFeatures(10000)
    val nb = new NaiveBayes().setModelType("multinomial").setSmoothing(0.001)//smoothing for words that exists only in test data
    /*val lr = new LogisticRegression().setMaxIter(100).setRegParam(0.03).setFamily("multinomial").setElasticNetParam(0.8)
    val ovr = new OneVsRest().setClassifier(lr)*/

    //convert back from ints to actual segment labels
    val predicteConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(indexer.labels)

    CacheHolder.labels_=(indexer.labels)

    val pipeline = new Pipeline().setStages(Array(indexer, tokenizer, hashingTF, nb, predicteConverter))
    val model = pipeline.fit(trainingDF)

    model
  }


  @RequestMapping(Array("/testModel"))
  def testModel(): String ={

    //take 20% percent of the data for evaluation since it failed on the classifyData
    val Array(trainingData, testData) = CacheHolder.appDescriptions.randomSplit(Array(0.8, 0.2),seed = 12345L) //seed for reproducibility)

    val predictions = CacheHolder.model.transform(testData)

    //evaluate results
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setPredictionCol("prediction")


    //evaluation of the model
    val sb:StringBuilder = new StringBuilder()
    sb.append(s"Accuracy: ${evaluator.setMetricName("accuracy").evaluate(predictions)}\n")
    sb.append(s"Precision: ${evaluator.setMetricName("weightedPrecision").evaluate(predictions)}\n")
    sb.append(s"Recall: ${evaluator.setMetricName("weightedRecall").evaluate(predictions)}\n")
    sb.append(s"F1: ${evaluator.setMetricName("f1").evaluate(predictions)}")

    val modelResults = sb.toString()

    modelResults
  }
}
