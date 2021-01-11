package com.littlecontrib.spark.nneighbor.node

import breeze.linalg.{CSCMatrix, DenseMatrix, Vector, argsort}

class SparseVecNodeNeighborFinder extends NodeNeighborFinder {
  def cosineCSCMatrixSimilarity(
    embeddings: CSCMatrix[Double]
  ): DenseMatrix[Double] = {
    val copyEmbedding = embeddings.copy
    copyEmbedding :*= copyEmbedding
    val norms = DenseMatrix.zeros[Double](embeddings.rows, 1)
    copyEmbedding.activeIterator
      .foreach {
        case ((row, _), value) => norms.update(row, 0, value + norms(row, 0))
      }
    norms.activeIterator
      .foreach {
        case ((row, _), value) => norms.update(row, 0, Math.sqrt(value))
      }
    (embeddings * embeddings.t).toDense /:/ (norms * norms.t).toDenseMatrix
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
    val rowCount = vecMap.size
    val colCount = vecMap(vecId2Index.head._1).size

    val embeddingMatrix = CSCMatrix.zeros[Double](rowCount, colCount)
    vecId2Index.foreach {
      case (vid, i) =>
        vecMap(vid).activeIterator
          .foreach(elem => embeddingMatrix.update(i, elem._1, elem._2))
    }
    val similarMartrix = cosineCSCMatrixSimilarity(embeddingMatrix)
    vecId2Index.par.map { // `.par` is important, will speed up this part in spark3
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
