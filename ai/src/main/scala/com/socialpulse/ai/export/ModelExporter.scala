package com.socialpulse.ai.export

import ml.dmlc.xgboost4j.scala.spark.XGBoostClassificationModel

object ModelExporter {

  /**
   * Exports the trained XGBoost model to a native format (.json or .ubj) 
   * so it can be loaded directly by the Spring Boot backend without Spark.
   */
  def exportNativeModel(model: XGBoostClassificationModel, path: String): Unit = {
    // XGBoost4J-Spark allows getting the underlying native booster
    val booster = model.nativeBooster
    
    // Save to native format (Spring Boot uses xgboost-predictor-java to load this)
    booster.saveModel(path)
  }
}
