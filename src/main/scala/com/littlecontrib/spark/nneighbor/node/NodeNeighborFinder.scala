package com.littlecontrib.spark.nneighbor.node

import breeze.linalg.Vector

trait NodeNeighborFinder {

  /**
    *
    * @param vecMap key is vector id, value is
    * @param k the k nearest neighbors
    * @return  Map, key is vector id, value is (neighbor_vector_id, similar_score)
    */
  def findNearest(vecMap: Map[String, Vector[Double]],
                  k: Int): Map[String, Seq[(String, Double)]]
}
