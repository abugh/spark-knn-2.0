package com.littlecontrib.spark.nneighbor.node

import breeze.linalg.{*, DenseMatrix, Vector, argsort, sum}
import breeze.numerics.sqrt

class DenseVecNodeNeighborFinder extends NodeNeighborFinder {

  def cosineSimilarity(embeddings: DenseMatrix[Double]): DenseMatrix[Double] = {
    val mSq = embeddings *:* embeddings
    val norms = sqrt(sum(mSq(::, *)).t)
    (embeddings.t * embeddings) /:/ (norms * norms.t)
  }

  def toDenseMatrix(
    featureMap: Map[String, Vector[Double]]
  ): DenseMatrix[Double] = {
    val ids = featureMap.keySet.toSeq
    val numvec = ids.length
    val numDim = featureMap(ids.head).length
    val embeddingMatrix = DenseMatrix.zeros[Double](numvec, numDim)
    ids.zipWithIndex.foreach {
      case (vid, i) => embeddingMatrix(::, i) := featureMap(vid)
    }
    embeddingMatrix
  }

  /**
    *
    * @param vecMap key is vector id, value is
    * @param k      the k nearest neighbors
    * @return Map, key is vector id, value is (neighbor_vector_id, similar_score)
    */
  override def findNearest(vecMap: Map[String, Vector[Double]],
                           k: Int): Map[String, Seq[(String, Double)]] = {
    if (vecMap.size == 0) {
      return Map.empty[String, Seq[(String, Double)]]
    }
    val vecId2Index = vecMap.keys.toSeq.zipWithIndex.toMap[String, Int]
    val index2VecId = vecId2Index.map(e => (e._2, e._1))
    val similarMartrix = cosineSimilarity(toDenseMatrix(vecMap))
    vecId2Index.par.map {
      case (vId, index) =>
        val vectorScorePairs =
          argsort(similarMartrix(::, index)).reverse
            .filter(index => !index2VecId(index).equals(vId))
            .slice(0, k)
            .map(
              vecIndex =>
                (index2VecId(vecIndex), similarMartrix(index, vecIndex))
            )
        vId -> vectorScorePairs
    }.seq
  }
}
