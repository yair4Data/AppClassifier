package com.startapp.service

import com.startapp.spring.Logging
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

/**
  * Created by yairf on 14/02/2018.
  */

@RestController
class GetAllSegments extends Logging{


  @RequestMapping(Array("/getSegments"))
  def getSegments(): String = {

    val spark = CacheHolder.spark

    CacheHolder.appDescriptions.createOrReplaceTempView("trainingData")

    val results = spark.sqlContext.sql("select distinct segment from trainingData")

    "["+results.collect().map(_.get(0)).mkString(",")+"]"

  }
}