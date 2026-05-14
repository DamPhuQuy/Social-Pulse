package com.socialpulse.ai.data.ingestion

import org.apache.spark.sql.{DataFrame, SparkSession}

object DataLoader {
  
  /**
   * Loads submissions JSON from the given path.
   */
  def loadSubmissions(spark: SparkSession, path: String): DataFrame = {
    spark.read.json(path)
  }

  /**
   * Loads comments JSON from the given path.
   */
  def loadComments(spark: SparkSession, path: String): DataFrame = {
    spark.read.json(path)
  }
}
