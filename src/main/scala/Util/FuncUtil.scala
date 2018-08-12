package Util

import com.startapp.service.CacheHolder
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.sql.functions.udf

/**
  * Created by yairf on 15/02/2018.
  */
object FuncUtil  extends Serializable{


  def topXPredictions(v: Seq[Double], labels: Broadcast[Array[String]], topX: Int): Array[String] = {
    val labelVal = labels.value
    v.toArray
      .zip(labelVal) //match the segment labels to the raw predictions
      .sortBy {
        case (score, label) => score    // sort ascending according to prediction value
      }
      .reverse // we want in descending order
      .map {
        case (score, label) => label //we are interested only in the labels , not the actual numeric prediction
      }
      .take(topX)
  }
  val densevecToSeq = udf((v: DenseVector) => v.toArray)  //convert the rawpredictions from vector to array
  val get_top_predictions = udf((v: Seq[Double], x: Int) => topXPredictions(v, CacheHolder.spark.sparkContext.broadcast(CacheHolder.labels), x))

  val getNull = udf(() => "": String)

  val toArray = udf((b: String) => b.split(",").map(_.toString))

}
