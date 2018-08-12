package com.startapp.service

import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Created by yairf on 13/02/2018.
  */
object CacheHolder  extends Serializable{

  private[this] var _spark: SparkSession = null

  private[this] var _labels: Array[String] = null

  private[this] var _appDescriptions: DataFrame = null

  private[this] var _model: PipelineModel = null


  def spark: SparkSession = _spark

  def spark_=(value: SparkSession): Unit = {
    _spark = value
  }

  def labels: Array[String] = _labels

  def labels_=(value: Array[String]): Unit = {
    _labels = value
  }

  def model: PipelineModel = _model

  def model_=(value: PipelineModel): Unit = {
    _model = value
  }

  def appDescriptions: DataFrame = _appDescriptions

  def appDescriptions_=(value: DataFrame): Unit = {
    _appDescriptions = value
  }
}
