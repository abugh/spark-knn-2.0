package com.littlecontrib.spark.nneighbor
import breeze.linalg.{
  DenseVector => BreezeDenseVec,
  SparseVector => BreezeSparseVec,
  Vector => BreezeVector
}
import org.apache.spark.ml.linalg.{
  DenseVector => SparkDenseVector,
  SparseVector => SparkSparseVector,
  Vector => SparkVec
}
object VecUtils {
  def toBreezeVec(vec: SparkVec): BreezeVector[Double] = {
    vec match {
      case v: SparkSparseVector =>
        val sparseVector = vec.toSparse
        BreezeSparseVec(v.size)(v.indices.zip(sparseVector.values): _*)
      case v: SparkDenseVector => BreezeDenseVec(v.toArray)
    }
  }
}
