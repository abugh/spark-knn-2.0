package com.littlecontrib.spark.nneighbor
import org.apache.spark.ml.linalg.{Vector => SparkVec}
import com.littlecontrib.spark.nneighbor.node.NodeNeighborFinder
import org.apache.spark.sql.{Dataset, Row, functions => F}
case class SimpleVector(vecId: String, vec: SparkVec)

case class VectorSimilarity(vecId: String, neighborId: String, score: Double)
case class SparkNearestNeighborFinder(localFinder: NodeNeighborFinder) {

  val findKNN = F.udf { (embeddingA: Seq[Row], embeddingB: Seq[Row], k: Int) =>
    val embeddingMap = embeddingA
      .map(em => {
        val uid = em.getAs[String](0)
        val vec = VecUtils.toBreezeVec(em.getAs[SparkVec](1))
        (uid, vec)
      })
      .toMap ++ embeddingB
      .map(em => {
        val uid = em.getAs[String](0)
        val vec = VecUtils.toBreezeVec(em.getAs[SparkVec](1))
        (uid, vec)
      })
      .toMap
    localFinder
      .findNearest(embeddingMap, k)
      .flatMap(e => {
        e._2.map(neighbor => VectorSimilarity(e._1, neighbor._1, neighbor._2))
      }).toSeq
  }

  def findNN(embedding: Dataset[SimpleVector],
             k: Int,
             splitNum: Int): Dataset[VectorSimilarity] = {
    import embedding.sparkSession.implicits._
    val embeddingPartitioned = embedding
      .toDF("vecId", "vec")
      .withColumn(
        "split_index",
        F.abs(F.hash($"vecId"))
          % splitNum
      )
      //split the embedding dataset to $splitNum pieces
      .groupBy($"split_index")
      .agg(F.collect_list(F.struct("vecId", "vec")).as("embeddings"))
    embeddingPartitioned
      .crossJoin(
        // make each pieces meet to each other
        embeddingPartitioned
          .withColumnRenamed("embeddings", "embeddings_b")
          .withColumnRenamed("split_index", "split_index_b")
      )

      //drop the duplicated pair of pieces
      .withColumn(
        "split_key",
        F.when(
            $"split_index" < $"split_index_b",
            F.concat_ws("_", $"split_index", $"split_index_b")
          )
          .otherwise(F.concat_ws("_", $"split_index_b", $"split_index"))
      )
      .dropDuplicates("split_key")
      .withColumn(
        "knns",
        findKNN($"embeddings", $"embeddings_b", F.lit(k))
      )
      .select(F.explode($"knns").as("knn"))
      .select(
        $"knn.vecId".as("vecId"),
        $"knn.neighborId".as("neighborId"),
        $"knn.score".as("score")
      )
      .dropDuplicates("vecId", "neighborId")
      .as[VectorSimilarity]
  }
}
