package com.littlecontrib.spark.neighbor
import org.apache.spark.ml.linalg.{Vectors => SparkVecs}
import com.holdenkarau.spark.testing.DataFrameSuiteBase
import com.littlecontrib.spark.nneighbor.{SimpleVector, SparkNearestNeighborFinder}
import com.littlecontrib.spark.nneighbor.node.DenseVecNodeNeighborFinder
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, CancelAfterFailure, FunSuite}

class Example extends FunSuite
  with BeforeAndAfter
  with CancelAfterFailure
  with BeforeAndAfterAll
  with DataFrameSuiteBase
  with Serializable {

  test("combineSeqsUdf") {
    val sparkConf = new SparkConf()
      .setMaster("local[2]")
      .setAppName("example")

    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    import spark.implicits._
    val vec = Seq(
      ("1", SparkVecs.dense(Array[Double](1d, 4d, 6d))),
      ("2", SparkVecs.dense(Array[Double](0d, 2d, 2d))),
      ("3", SparkVecs.dense(Array[Double](5d, 1d, 5d))),
      ("4", SparkVecs.dense(Array[Double](4.3d, 2.8d, 9d))),
      ("5", SparkVecs.dense(Array[Double](0.22d, 4d, 5.8d))),
      ("6", SparkVecs.dense(Array[Double](1.2d, 4.4d, 6.2d))),
      ("7", SparkVecs.dense(Array[Double](2d, 3d, 4d))),
      ("8", SparkVecs.dense(Array[Double](6d, 3d, 2d))),
      ("9", SparkVecs.dense(Array[Double](7d, 0d, 0d)))
    ).toDF("vecId","vec").as[SimpleVector]
    val finder = SparkNearestNeighborFinder(
      DenseVecNodeNeighborFinder()
    )
    val sim = finder.findNN(vec, 2, 2).collect()
    println("source_id, neighbor_id,score")
    sim.foreach(e=>println(e.vecId+","+e.neighborId+","+e.score))
  }
}
