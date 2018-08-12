package com.startapp

/**
  * Created by yairf on 10/02/2018.
  */




import java.nio.charset.Charset

import TextUtilities.TextCleaner.cleanText
import TextUtilities.TextTools.lemmatizeText
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{LogisticRegression, NaiveBayes, OneVsRest}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.sql.types.StringType

object AppClassifierNaive {

  val asciiEncoder = Charset.forName("US-ASCII").newEncoder
  val isoEncoder = Charset.forName("ISO-8859-1").newEncoder

  val IS_ENGLISH_REGEX = "^[a-zA-Z0-9$@$!%*?&#^-_. +]+$" //"^[ \\w \\d \\s \\. \\& \\+ \\- \\, \\! \\@ \\# \\$ \\% \\^ \\* \\( \\) \\; \\\\ \\/ \\| \\< \\> \\\" \\' \\? \\= \\: \\[ \\] ]*$"

  val preProcessText = udf { txt: String =>{
      var text = cleanText(txt)
      if (text.length > 0) text = lemmatizeText(text).toString
      text
    }
  }
  //def filterNonEnglish: String => String = _.replaceAll("^[a-zA-Z0-9$@$!%*?&#^-_. +]+$", "")
  /*val filterNonEnglish= udf { txt: String =>{
    //txt.replaceAll("^[a-zA-Z0-9$@$!%*?&#^-_. +]+$", "") //
    var text  = txt.replaceAll("[^\\x00-\\x7F]", "");//
    text
    }
  }*/

  val  isEnglish = udf { text: String => {
      asciiEncoder.canEncode(text) //|| isoEncoder.canEncode(text)
    }
  }

 /* implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }
  val checkEnglishTxt = udf {
    txt: String =>{
      //val pattern = "[^\\x00-\\x7F]".r
      val nonenglishText = txt match {
        //case r"[^\\x00-\\x7F]" => false
        case IS_ENGLISH_REGEX => false
        case _ => true
      }
      s"nonenglishText" + nonenglishText + " for : "+txt
      nonenglishText
    }
  }*/

  def overrideNonEnglishCategory(txt: String): String = {
    if (asciiEncoder.canEncode(txt)) txt else
      "Not supported language"
  }
  def preProcessText(txt: String): String = {
    var text = cleanText(txt)
    if (text.length > 0) text = lemmatizeText(text).toString
    text
  }

  def topXPredictions(v: Vector, labels: Broadcast[Array[String]], topX: Int): Array[String] = {
    val labelVal = labels.value
    v.toArray
      .zip(labelVal)
      .sortBy {
        case (score, label) => score
      }
      .reverse
      .map {
        case (score, label) => label
      }
      .take(topX)
  }

  //val get_top_predictions = udf((v: Vector, x: Int) => topXPredictions(v, labelsBroadcast, x))

  /*val function: (String => Boolean) = (txt: String) => {
    val nonenglishText = txt.matches("[^\\x00-\\x7F]")
    s"nonenglishText" + nonenglishText + " for : "+txt
    nonenglishText
  }
  val udfFilteringNonEnglish = udf(function)
*/
  val getNull = udf(() => "": String)

  val toArray = udf((b: String) => b.split(",").map(_.toString))

  def main(args: Array[String]) {
    //val conf = new SparkConf().setMaster("local[*]").setAppName("CorpusCleaner")
    //val sc = new SparkContext(conf)
    val spark: SparkSession = SparkSession
      .builder()
      .master("local[*]")
      //.master("spark://saas-logs-lab-master.cws.lab:7077")
      .config("spark.sql.warehouse.dir", "file:///C:\\Workspace-saas-logs-ng\\AppClassifier")
      //.config("spark.sql.warehouse.dir", "/var/tmp")
      .appName("AppClassifier")
      //.config("spark.submit.deployMode", "cluster")
      //.config("spark.executor.memory", "24g")
      //.config("spark.driver.memory", "2g")
      .config("spark.serializer", classOf[KryoSerializer].getName)
      //.config("spark.driver.maxResultSize", "2G")
      /*.config("spark.submit.deployMode", deployMode)
      .config("spark.executor.memory", executorMemory)
      .config("spark.driver.memory", driverMemory)*/
      .getOrCreate()

    val sqlContext = spark.sqlContext//new SQLContext(sc)

    //val tweets_path = "data/sample_data.txt"
    //val appDescriptions = spark.read.option("sep", "|").textFile("Examples.txt")
    val appDescriptions = sqlContext.read.format("csv").option("header", "true").
      option("delimiter", "|").option("inferSchema","true").load("Examples.txt")
    /*val appDescriptions = spark.read.format("com.crealytics.spark.excel")
      .option("sheetName", "Sheet1")
      .option("useHeader", "true")
      .option("treatEmptyValuesAsNulls", "false")
      .option("inferSchema", "true")
      .load("appDescriptions.xlsx")*/
    //.option("location", "appDescriptions.xlsx")

    //appDescriptions.show()

    val justEnglishDesc = appDescriptions.filter(isEnglish(col("description")))
    //val newAppDesc = appDescriptions.withColumn("eng_description" ,filterNonEnglish(col("description")))
    //appDescriptions.withColumn("clean_description",preProcessText(col("eng_description")))

    //justEnglishDesc.show(30)

    val justEnglishDescArray = justEnglishDesc.withColumn("descArray",toArray(col("description")))

    val remover = new StopWordsRemover()
      .setInputCol("descArray")
      .setOutputCol("filter_dtopword_description")

    val filteredDesc = remover.transform(justEnglishDescArray)//.show(true)//.drop(Col("descArray"))//.show(true)

    //filteredDesc.show(2)

    val descToStr = filteredDesc.withColumn("filter_str_description", concat_ws(",", col("filter_dtopword_description")))
    val trainingDF = descToStr.withColumn("clean_description",preProcessText(col("filter_str_description"))).drop(col("descArray")).drop(col("filter_str_description")).drop(col("filter_dtopword_description"))


    trainingDF.show(5)

    // Model Architecture

    //convert the String Category feature intlplan and label into number indices (allows decision trees to improve performance
    val indexer = new StringIndexer().setInputCol("segment").setOutputCol("label").fit(trainingDF)
    //breaking the description into individual terms
    val tokenizer = new Tokenizer().setInputCol("clean_description").setOutputCol("tokens")
    val hashingTF = new HashingTF().setInputCol("tokens").setOutputCol("features").setNumFeatures(10000)
    val nb = new NaiveBayes().setModelType("multinomial")
    /*val lr = new LogisticRegression().setMaxIter(100).setRegParam(0.03).setFamily("multinomial").setElasticNetParam(0.8)
    val ovr = new OneVsRest().setClassifier(lr)*/
    val predicteConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(indexer.labels)

    /*val segmentConverter = new IndexToString()
      .setInputCol("label")
      .setOutputCol("segmentOrig")
      .setLabels(indexer.labels)*/

    val pipeline = new Pipeline().setStages(Array(indexer, tokenizer, hashingTF, nb, /*ovr ,*/predicteConverter/*, segmentConverter*/))
    val model = pipeline.fit(trainingDF)


    val appToClassify = sqlContext.read.format("csv").option("header", "true").
      option("delimiter", "|").option("inferSchema","true").load("Classify.txt")

    val justEnglishDescToClassify = appToClassify.filter(isEnglish(col("description")))

    val justEnglishDescClassifyArray = justEnglishDescToClassify.withColumn("descArray",toArray(col("description")))


    val filteredDescClassify = remover.transform(justEnglishDescClassifyArray)

    val descToStrClassify = filteredDescClassify.withColumn("filter_str_description", concat_ws(",", col("filter_dtopword_description")))
    val classifygDF = descToStrClassify.withColumn("clean_description",preProcessText(col("filter_str_description"))).drop(col("descArray")).drop(col("filter_str_description")).drop(col("filter_dtopword_description"))
                      .withColumn("segment", getNull())


    classifygDF.show(1)
    //// Set up Evaluator (prediction, true label)
    /*val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("segment")
      .setRawPredictionCol("prediction")*/


    val prediction = model.transform(classifygDF).select("clean_description", "description","appId","prediction","predictedLabel")

    //override non-english categories
    prediction.withColumn("predictedLabelFinal", when(isEnglish(col("description")), col("predictedLabel")).otherwise("Not supported language"))

    prediction.show(2)
    //val accuracy = evaluator.evaluate(prediction)

    //println("accuracy of model : "+accuracy)
    //evaluator.explainParams()

    //prediction.collect().foreach(row => println(row))

    // Instantiate metrics object


    val evaluator = new MulticlassClassificationEvaluator()
      //.setLabelCol("predictedLabel")
      .setLabelCol("label")
      .setPredictionCol("prediction")
      //.setMetricName("precision")

    val modelOntraining = model.transform(trainingDF)

    modelOntraining.describe().show()
    //val accuracy = evaluator.evaluate(modelOntraining)


    println(s"Accuracy: ${evaluator.setMetricName("accuracy").evaluate(modelOntraining)}")
    println(s"Precision: ${evaluator.setMetricName("weightedPrecision").evaluate(modelOntraining)}")
    println(s"Recall: ${evaluator.setMetricName("weightedRecall").evaluate(modelOntraining)}")
    println(s"F1: ${evaluator.setMetricName("f1").evaluate(modelOntraining)}")


   // prediction.createOrReplaceTempView("results")

   // val results = sqlContext.sql("select description,appId,predictedLabel from results")

   // results.show(100/*,false*/)

  }
}
