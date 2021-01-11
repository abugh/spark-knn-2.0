package com.littlecontrib.spark.nneighbor.node

import breeze.linalg

class ANNNodeNeighborFinder extends NodeNeighborFinder {

  /**
    *
    * @param vecMap key is vector id, value is
    * @param k      the k nearest neighbors
    * @return Map, key is vector id, value is (neighbor_vector_id, similar_score)
    */
  override def findNearest(vecMap: Map[String, linalg.Vector[Double]],
                           k: Int): Map[String, Seq[(String, Double)]] = {
    Map.empty[String, Seq[(String, Double)]]
  }
}
