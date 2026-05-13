package com.socialpulse.ai.model.trainer

import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.feature.VectorAssembler
import ml.dmlc.xgboost4j.scala.spark.XGBoostClassifier

object RankerTrainer {

  /**
   * Trains an XGBoost model on the extracted features.
   */
  def trainModel(trainingData: DataFrame, numRounds: Int = 100, maxDepth: Int = 5) = {
    // 1. Assemble features into a vector
    val featureCols = Array("upvoteCount", "cmtCount", "contentLength")
    val assembler = new VectorAssembler()
      .setInputCols(featureCols)
      .setOutputCol("features")

    val assembledData = assembler.transform(trainingData)

    // Add a dummy label for now (e.g. 1 if they commented, 0 if negative sample)
    // In reality, we need to generate negative samples (posts the user saw but didn't comment on)
    val dataWithLabel = assembledData.withColumn("label", org.apache.spark.sql.functions.lit(1.0))

    // 2. Configure XGBoost
    val xgbParam = Map(
      "objective" -> "binary:logistic",
      "eval_metric" -> "auc",
      "max_depth" -> maxDepth,
      "eta" -> 0.1,
      "num_round" -> numRounds
    )

    val xgbClassifier = new XGBoostClassifier(xgbParam)
      .setFeaturesCol("features")
      .setLabelCol("label")

    // 3. Train
    val model = xgbClassifier.fit(dataWithLabel)
    model
  }
}
