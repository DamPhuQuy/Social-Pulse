package com.socialpulse.ai.features.extractor

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

object FeatureExtractor {

  /**
   * Generates Post Features, User Features, and Interaction Features 
   * by joining submissions and comments.
   *
   * @param submissions DataFrame of submissions
   * @param comments DataFrame of comments
   * @return DataFrame containing joined features ready for ML
   */
  def buildFeatures(submissions: DataFrame, comments: DataFrame): DataFrame = {
    // 1. Extract Post Features
    // e.g. age, text length, score, num_comments
    val postFeatures = submissions.select(
      col("id").alias("post_id"),
      col("author").alias("post_author"),
      col("score").alias("upvoteCount"),
      col("num_comments").alias("cmtCount"),
      length(col("selftext")).alias("contentLength"),
      col("created_utc").alias("post_created_utc")
    )

    // 2. Extract Interaction Features from Comments
    // A comment from User A on Post P means User A interacted with Post P and Author B
    val interactions = comments
      .filter(col("author") =!= "[deleted]") // Ignore deleted authors
      .select(
        col("author").alias("viewer_id"),
        substring(col("link_id"), 4, 100).alias("post_id"), // Remove t3_ prefix
        col("created_utc").alias("interaction_time")
      )

    // Join interactions with post features
    val dataset = interactions.join(postFeatures, Seq("post_id"), "inner")
    
    // In a real scenario, we'd do window functions here to calculate 
    // past interaction counts (interactionCount7d) BEFORE the current interaction time
    // to avoid data leakage. For now, this is a skeleton.
    
    dataset
  }
}
